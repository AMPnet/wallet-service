package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.GrpcHandledException
import com.ampnet.walletservice.exception.InternalException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.service.BroadcastTransactionService
import com.ampnet.walletservice.service.CooperativeDepositService
import com.ampnet.walletservice.service.CooperativeWalletService
import com.ampnet.walletservice.service.CooperativeWithdrawService
import com.ampnet.walletservice.service.ProjectInvestmentService
import com.ampnet.walletservice.service.RevenueService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WalletService
import com.ampnet.walletservice.service.WithdrawService
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID
import javax.transaction.Transactional

@Service
@Suppress("TooManyFunctions")
class BroadcastTransactionServiceImpl(
    private val walletService: WalletService,
    private val withdrawService: WithdrawService,
    private val cooperativeWalletService: CooperativeWalletService,
    private val cooperativeWithdrawService: CooperativeWithdrawService,
    private val cooperativeDepositService: CooperativeDepositService,
    private val projectInvestmentService: ProjectInvestmentService,
    private val transactionInfoService: TransactionInfoService,
    private val revenueService: RevenueService
) : BroadcastTransactionService {

    companion object : KLogging()

    @Transactional
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun broadcast(txId: Int, signedTransaction: String): String {
        val transactionInfo = transactionInfoService.findTransactionInfo(txId)
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Non existing transaction with id: $txId")
        val coop = transactionInfo.coop
        logger.info { "Broadcasting transaction: $transactionInfo for coop: $coop" }

        val txHash = when (transactionInfo.type) {
            TransactionType.WALLET_ACTIVATE -> activateWallet(transactionInfo, signedTransaction)
            TransactionType.CREATE_ORG ->
                createOrganizationWallet(transactionInfo, signedTransaction, coop)
            TransactionType.CREATE_PROJECT ->
                createProjectWallet(transactionInfo, signedTransaction, coop)
            TransactionType.INVEST -> projectInvestmentService.investInProject(signedTransaction, coop)
            TransactionType.CANCEL_INVEST ->
                projectInvestmentService.cancelInvestmentsInProject(signedTransaction, coop)
            TransactionType.MINT -> confirmMintTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN_APPROVAL ->
                confirmApprovalTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN -> burnTransaction(transactionInfo, signedTransaction)
            TransactionType.REVENUE_PAYOUT -> revenuePayoutTransaction(transactionInfo, signedTransaction)
            TransactionType.TRNSF_TOKEN_OWN, TransactionType.TRNSF_PLTFRM_OWN ->
                transferOwnershipTransaction(transactionInfo, signedTransaction)
        }
        logger.info { "Successfully broadcast transaction. TxHash: $txHash" }
        transactionInfoService.deleteTransaction(transactionInfo.id)
        return txHash
    }

    private fun activateWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val walletUuid = getUuidFromCompanionData(transactionInfo)
        val wallet = cooperativeWalletService.activateWallet(walletUuid, signedTransaction)
        return wallet.hash ?: throw ResourceNotFoundException(
            ErrorCode.TX_MISSING, "Wallet: $wallet is missing hash"
        )
    }

    private fun createOrganizationWallet(
        transactionInfo: TransactionInfo,
        signedTransaction: String,
        coop: String
    ): String {
        val organization = getUuidFromCompanionData(transactionInfo)
        val wallet = walletService.createOrganizationWallet(organization, signedTransaction, coop)
        return wallet.activationData
    }

    private fun createProjectWallet(transactionInfo: TransactionInfo, signedTransaction: String, coop: String): String {
        val project = getUuidFromCompanionData(transactionInfo)
        val wallet = walletService.createProjectWallet(project, signedTransaction, coop)
        return wallet.activationData
    }

    private fun confirmMintTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val depositId = getIdFromCompanionData(transactionInfo)
        val deposit = cooperativeDepositService.confirmMintTransaction(
            transactionInfo.coop, signedTransaction, depositId
        )
        return deposit.txHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing txHash for mint transaction")
    }

    private fun confirmApprovalTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getIdFromCompanionData(transactionInfo)
        val withdraw = withdrawService.confirmApproval(signedTransaction, withdrawId, transactionInfo.coop)
        return withdraw.approvedTxHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing approvedTxHash for withdraw transaction")
    }

    private fun burnTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getIdFromCompanionData(transactionInfo)
        val withdraw = cooperativeWithdrawService.burn(signedTransaction, withdrawId, transactionInfo.coop)
        return withdraw.burnedTxHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing burnedTxHash for withdraw transaction")
    }

    private fun revenuePayoutTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val revenuePayoutId = getIdFromCompanionData(transactionInfo)
        val revenuePayout = revenueService.confirmRevenuePayout(signedTransaction, revenuePayoutId)
        return revenuePayout.txHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing txHash for revenue payout transaction")
    }

    private fun transferOwnershipTransaction(info: TransactionInfo, signed: String): String =
        cooperativeWalletService.transferOwnership(signed, info.coop)

    private fun getIdFromCompanionData(transactionInfo: TransactionInfo): Int {
        try {
            val companionData = transactionInfo.companionData
                ?: throw InvalidRequestException(
                    ErrorCode.TX_DATA_MISSING, "Missing id for ${transactionInfo.type}"
                )
            return companionData.toInt()
        } catch (ex: NumberFormatException) {
            throw InternalException(ErrorCode.INT_INVALID_VALUE, "Companion data is not Int")
        }
    }

    private fun getUuidFromCompanionData(transactionInfo: TransactionInfo): UUID {
        try {
            val companionData = transactionInfo.companionData
                ?: throw InvalidRequestException(
                    ErrorCode.TX_DATA_MISSING, "Missing uuid for ${transactionInfo.type}"
                )
            return UUID.fromString(companionData)
        } catch (ex: IllegalArgumentException) {
            throw InternalException(ErrorCode.INT_INVALID_VALUE, "Companion data is not UUID")
        }
    }
}
