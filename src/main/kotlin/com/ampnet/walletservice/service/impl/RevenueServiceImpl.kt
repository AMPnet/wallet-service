package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.GrpcHandledException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.RevenuePayoutTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.RevenuePayout
import com.ampnet.walletservice.persistence.repository.RevenuePayoutRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.RevenueService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.request.RevenuePayoutTxInfoRequest
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class RevenueServiceImpl(
    private val revenuePayoutRepository: RevenuePayoutRepository,
    private val walletRepository: WalletRepository,
    private val projectService: ProjectService,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService
) : RevenueService {

    companion object : KLogging()

    @Transactional
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun generateRevenuePayout(user: UserPrincipal, project: UUID, amount: Long): TransactionDataAndInfo {
        logger.info { "Generating revenue payout transaction info" }
        val projectResponse = projectService.getProject(project)
        ServiceUtils.validateUserIsProjectOwner(user.uuid, projectResponse)
        val userWallet = ServiceUtils.getWalletHash(user.uuid, walletRepository)
        val projectWallet = ServiceUtils.getWalletHash(project, walletRepository)
        validateProjectHasEnoughFunds(projectWallet, amount)

        val revenuePayout = RevenuePayout(project, amount, user.uuid, user.coop)
        revenuePayoutRepository.save(revenuePayout)
        val request = RevenuePayoutTxRequest(userWallet, projectWallet, amount)
        val data = blockchainService.generateRevenuePayout(request)
        val txInfoRequest = RevenuePayoutTxInfoRequest(projectResponse.name, amount, user, revenuePayout.id)
        val info = transactionInfoService.createRevenuePayoutTransaction(txInfoRequest)
        logger.info { "Successfully generate revenue payout: $revenuePayout" }
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, GrpcException::class, GrpcHandledException::class)
    override fun confirmRevenuePayout(signedTransaction: String, revenuePayoutId: Int): RevenuePayout {
        val revenuePayout = revenuePayoutRepository.findById(revenuePayoutId).orElseThrow {
            throw ResourceNotFoundException(
                ErrorCode.WALLET_PAYOUT_MISSING, "Missing RevenuePayout with id: $revenuePayoutId"
            )
        }
        val txHash = blockchainService.postTransaction(signedTransaction, revenuePayout.coop)
        revenuePayout.txHash = txHash
        revenuePayout.completedAt = ZonedDateTime.now()
        return revenuePayout
    }

    @Transactional(readOnly = true)
    override fun getRevenuePayouts(project: UUID, pageable: Pageable): Page<RevenuePayout> {
        return revenuePayoutRepository.findByProjectUuid(project, pageable)
    }

    private fun validateProjectHasEnoughFunds(projectWallet: String, amount: Long) {
        val balance = blockchainService.getBalance(projectWallet) ?: 0
        if (amount > balance) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "Insufficient funds for revenue payout")
        }
    }
}
