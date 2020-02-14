package com.ampnet.walletservice.service.impl

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.ProjectInvestmentService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.ProjectInvestmentRequest
import java.time.ZonedDateTime
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    @Throws(InvalidRequestException::class, ResourceNotFoundException::class)
    override fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo {
        logger.debug { "Generating Investment in project for request: $request" }
        val projectResponse = projectService.getProject(request.projectUuid)
        verifyProjectIsStillActive(projectResponse)
        verifyInvestmentAmountIsValid(projectResponse, request.amount)

        val userWalletHash = ServiceUtils.getWalletHash(request.investorUuid, walletRepository)
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
            projectResponse.name, request.amount, request.investorUuid)
        logger.debug { "Generated Investment in project for request: $request" }
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun generateCancelInvestmentsInProjectTransaction(
        projectUuid: UUID,
        userUuid: UUID
    ): TransactionDataAndInfo {
        logger.debug { "Generating cancel investments in project $projectUuid by user $userUuid" }
        val projectResponse = projectService.getProject(projectUuid)

        val userWalletHash = ServiceUtils.getWalletHash(userUuid, walletRepository)
        val projectWalletHash = ServiceUtils.getWalletHash(projectUuid, walletRepository)
        val data = blockchainService.generateCancelInvestmentsInProject(userWalletHash, projectWalletHash)
        val info = transactionInfoService.cancelInvestmentTransaction(projectResponse.name, userUuid)
        logger.debug { "Generated cancel investments in project $projectUuid by user $userUuid" }
        return TransactionDataAndInfo(data, info)
    }

    override fun investInProject(signedTransaction: String): String =
        blockchainService.postTransaction(signedTransaction)

    override fun cancelInvestmentsInProject(signedTransaction: String): String =
        blockchainService.postTransaction(signedTransaction)

    private fun verifyProjectIsStillActive(project: ProjectResponse) {
        if (project.active.not()) {
            throw InvalidRequestException(ErrorCode.PRJ_NOT_ACTIVE, "Project is not active")
        }
        if (ZonedDateTime.now().toInstant().toEpochMilli() > project.endDate) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE_EXPIRED, "Project has expired at: ${project.endDate}")
        }
    }

    private fun verifyInvestmentAmountIsValid(project: ProjectResponse, amount: Long) {
        if (amount > project.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_PER_USER, "User can invest max ${project.maxPerUser}")
        }
        if (amount < project.minPerUser) {
            throw InvalidRequestException(
                ErrorCode.PRJ_MIN_PER_USER, "User has to invest at least ${project.minPerUser}")
        }
    }

    private fun verifyUserHasEnoughFunds(hash: String, amount: Long) {
        val funds = blockchainService.getBalance(hash)
        if (funds < amount) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "User does not have enough funds on wallet")
        }
    }

    private fun verifyProjectDidNotReachExpectedInvestment(hash: String, expectedFunding: Long) {
        val currentFunds = blockchainService.getBalance(hash)
        if (currentFunds == expectedFunding) {
            throw InvalidRequestException(
                ErrorCode.PRJ_MAX_FUNDS, "Project has reached expected funding: $currentFunds")
        }
    }
}
