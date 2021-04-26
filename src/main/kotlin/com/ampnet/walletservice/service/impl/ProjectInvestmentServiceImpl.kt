package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.GrpcHandledException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.ProjectInvestmentService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.request.ProjectInvestmentRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ProjectInvestmentServiceImpl(
    private val walletRepository: WalletRepository,
    private val blockchainService: BlockchainService,
    private val projectService: ProjectService,
    private val transactionInfoService: TransactionInfoService
) : ProjectInvestmentService {

    companion object : KLogging()

    @Transactional
    @Throws(
        InvalidRequestException::class,
        ResourceNotFoundException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo {
        logger.debug { "Generating Investment in project for request: $request" }
        val projectResponse = projectService.getProject(request.projectUuid)
        if (projectResponse.expectedFunding == null || projectResponse.maxPerUser == null ||
            projectResponse.endDate == null
        ) throw InvalidRequestException(
            ErrorCode.PRJ_MISSING_INFO, "Project:$projectResponse is missing information to place investment."
        )
        verifyProjectIsStillActive(projectResponse.active, projectResponse.endDate)
        verifyInvestmentAmountIsValid(request.amount, projectResponse.maxPerUser)

        val userWalletHash = ServiceUtils.getWalletHash(request.investor.uuid, walletRepository)
        verifyUserHasEnoughFunds(userWalletHash, request.amount)

        val projectWalletHash = ServiceUtils.getWalletHash(request.projectUuid, walletRepository)
        verifyProjectDidNotReachExpectedInvestment(projectWalletHash, projectResponse.expectedFunding)

        val investRequest = ProjectInvestmentTxRequest(
            userWalletHash,
            projectWalletHash,
            request.amount
        )
        val data = blockchainService.generateProjectInvestmentTransaction(investRequest)
        val info = transactionInfoService.createInvestTransaction(
            projectResponse.name, request.amount, request.investor
        )
        logger.debug { "Generated Investment in project for request: $request" }
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun generateCancelInvestmentsInProjectTransaction(
        projectUuid: UUID,
        user: UserPrincipal
    ): TransactionDataAndInfo {
        logger.debug { "Generating cancel investments in project $projectUuid by user ${user.uuid}" }
        val projectResponse = projectService.getProject(projectUuid)

        val userWalletHash = ServiceUtils.getWalletHash(user.uuid, walletRepository)
        val projectWalletHash = ServiceUtils.getWalletHash(projectUuid, walletRepository)
        val data = blockchainService.generateCancelInvestmentsInProject(userWalletHash, projectWalletHash)
        val info = transactionInfoService.cancelInvestmentTransaction(projectResponse.name, user)
        logger.debug { "Generated cancel investments in project $projectUuid by user ${user.uuid}" }
        return TransactionDataAndInfo(data, info)
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun investInProject(signedTransaction: String, coop: String): String =
        blockchainService.postTransaction(signedTransaction, coop)

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun cancelInvestmentsInProject(signedTransaction: String, coop: String): String =
        blockchainService.postTransaction(signedTransaction, coop)

    private fun verifyProjectIsStillActive(active: Boolean, endDate: ZonedDateTime) {
        if (active.not()) {
            throw InvalidRequestException(ErrorCode.PRJ_NOT_ACTIVE, "Project is not active")
        }
        if (endDate.isBefore(ZonedDateTime.now())) {
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")
            val endDateFormatted = endDate.format(dateFormatter)
            throw InvalidRequestException(ErrorCode.PRJ_DATE_EXPIRED, "Project has expired at: $endDateFormatted")
        }
    }

    private fun verifyInvestmentAmountIsValid(amount: Long, maxPerUser: Long) {
        if (amount > maxPerUser) {
            throw InvalidRequestException(
                ErrorCode.PRJ_MAX_PER_USER,
                "User can invest max ${maxPerUser.toEurAmount()}"
            )
        }
    }

    private fun verifyUserHasEnoughFunds(hash: String, amount: Long) {
        val funds = blockchainService.getBalance(hash) ?: 0
        if (funds < amount) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "User does not have enough funds on wallet")
        }
    }

    private fun verifyProjectDidNotReachExpectedInvestment(hash: String, expectedFunding: Long) {
        val currentFunds = blockchainService.getBalance(hash)
        if (currentFunds == expectedFunding) {
            throw InvalidRequestException(
                ErrorCode.PRJ_MAX_FUNDS, "Project has reached expected funding: ${currentFunds.toEurAmount()}"
            )
        }
    }
}
