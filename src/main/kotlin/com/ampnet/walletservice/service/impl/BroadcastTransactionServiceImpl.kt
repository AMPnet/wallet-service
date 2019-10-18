package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InternalException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.service.BroadcastTransactionService
import com.ampnet.walletservice.service.CooperativeWalletService
import com.ampnet.walletservice.service.DepositService
import com.ampnet.walletservice.service.ProjectInvestmentService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WalletService
import com.ampnet.walletservice.service.WithdrawService
import java.util.UUID
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class BroadcastTransactionServiceImpl(
    private val walletService: WalletService,
    private val cooperativeWalletService: CooperativeWalletService,
    private val depositService: DepositService,
    private val withdrawService: WithdrawService,
    private val projectInvestmentService: ProjectInvestmentService,
    private val transactionInfoService: TransactionInfoService
) : BroadcastTransactionService {

    companion object : KLogging()

    override fun broadcast(txId: Int, signedTransaction: String): String {
        val transactionInfo = transactionInfoService.findTransactionInfo(txId)
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Non existing transaction with id: $txId")
        logger.info { "Broadcasting transaction: $transactionInfo" }

        val txHash = when (transactionInfo.type) {
            TransactionType.WALLET_ACTIVATE -> activateWallet(transactionInfo, signedTransaction)
            TransactionType.CREATE_ORG -> createOrganizationWallet(transactionInfo, signedTransaction)
            TransactionType.CREATE_PROJECT -> createProjectWallet(transactionInfo, signedTransaction)
            TransactionType.INVEST -> projectInvestmentService.investInProject(signedTransaction)
            TransactionType.MINT -> confirmMintTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN_APPROVAL ->
                confirmApprovalTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN -> burnTransaction(transactionInfo, signedTransaction)
        }
        logger.info { "Successfully broadcast transaction. TxHash: $txHash" }
        transactionInfoService.deleteTransaction(transactionInfo.id)
        return txHash
    }

    private fun activateWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val walletUuid = getUuidFromCompanionData(transactionInfo)
        val wallet = cooperativeWalletService.activateWallet(walletUuid, signedTransaction)
        return wallet.hash ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Wallet: $wallet is missing hash")
    }

    private fun createOrganizationWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val organization = getUuidFromCompanionData(transactionInfo)
        val wallet = walletService.createOrganizationWallet(organization, signedTransaction)
        return wallet.activationData
    }

    private fun createProjectWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val project = getUuidFromCompanionData(transactionInfo)
        val wallet = walletService.createProjectWallet(project, signedTransaction)
        return wallet.activationData
    }

    private fun confirmMintTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val depositId = getIdFromCompanionData(transactionInfo)
        val deposit = depositService.confirmMintTransaction(signedTransaction, depositId)
        return deposit.txHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing txHash for mint transaction")
    }

    private fun confirmApprovalTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getIdFromCompanionData(transactionInfo)
        val withdraw = withdrawService.confirmApproval(signedTransaction, withdrawId)
        return withdraw.approvedTxHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing approvedTxHash for withdraw transaction")
    }

    private fun burnTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getIdFromCompanionData(transactionInfo)
        val withdraw = withdrawService.burn(signedTransaction, withdrawId)
        return withdraw.burnedTxHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing burnedTxHash for withdraw transaction")
    }

    private fun getIdFromCompanionData(transactionInfo: TransactionInfo): Int {
        try {
            val companionData = transactionInfo.companionData
                ?: throw InvalidRequestException(
                    ErrorCode.TX_COMPANION_DATA_MISSING, "Missing id for ${transactionInfo.type}")
            return companionData.toInt()
        } catch (ex: NumberFormatException) {
            throw InternalException(ErrorCode.INT_INVALID_VALUE, "Companion data is not Int")
        }
    }

    private fun getUuidFromCompanionData(transactionInfo: TransactionInfo): UUID {
        try {
            val companionData = transactionInfo.companionData
                ?: throw InvalidRequestException(
                    ErrorCode.TX_COMPANION_DATA_MISSING, "Missing uuid for ${transactionInfo.type}")
            return UUID.fromString(companionData)
        } catch (ex: IllegalArgumentException) {
            throw InternalException(ErrorCode.INT_INVALID_VALUE, "Companion data is not UUID")
        }
    }
}
