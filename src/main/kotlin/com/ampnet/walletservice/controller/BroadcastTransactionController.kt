package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TxHashResponse
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InternalException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.service.CooperativeWalletService
import com.ampnet.walletservice.service.DepositService
import com.ampnet.walletservice.service.ProjectInvestmentService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WalletService
import com.ampnet.walletservice.service.WithdrawService
import com.ampnet.walletservice.websocket.WebSocketNotificationService
import java.util.UUID
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class BroadcastTransactionController(
    private val transactionInfoService: TransactionInfoService,
    private val walletService: WalletService,
    private val cooperativeWalletService: CooperativeWalletService,
    private val projectInvestmentService: ProjectInvestmentService,
    private val depositService: DepositService,
    private val withdrawService: WithdrawService,
    private val notificationService: WebSocketNotificationService
) {

    companion object : KLogging()

    @PostMapping("/tx_broadcast")
    fun broadcastTransaction(
        @RequestParam(name = "tx_id", required = true) txId: Int,
        @RequestParam(name = "tx_sig", required = true) signedTransaction: String
    ): ResponseEntity<TxHashResponse> {
        logger.info { "Received request to broadcast transaction with id: $txId" }

        val transactionInfo = getTransactionInfo(txId)
        logger.info { "Broadcasting transaction: $transactionInfo" }

        val txHash = when (transactionInfo.type) {
            TransactionType.WALLET_ACTIVATE -> activateWallet(transactionInfo, signedTransaction)
            TransactionType.CREATE_ORG -> createOrganizationWallet(transactionInfo, signedTransaction)
            TransactionType.CREATE_PROJECT -> createProjectWallet(transactionInfo, signedTransaction)
            TransactionType.INVEST -> projectInvestmentService.investInProject(signedTransaction)
            TransactionType.MINT -> confirmMintTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN_APPROVAL -> confirmApprovalTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN -> burnTransaction(transactionInfo, signedTransaction)
        }
        logger.info { "Successfully broadcast transaction. TxHash: $txHash" }

        notificationService.notifyTxBroadcast(txId, "BROADCAST")

        transactionInfoService.deleteTransaction(transactionInfo.id)
        return ResponseEntity.ok(TxHashResponse(txHash))
    }

    private fun activateWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val companionData = transactionInfo.companionData
            ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_DATA_MISSING, "Missing wallet id")
        val walletUuid = getUuidFromCompanionData(companionData)
        val wallet = cooperativeWalletService.activateWallet(walletUuid, signedTransaction)
        return wallet.hash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Wallet: $wallet is missing hash")
    }

    private fun createOrganizationWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val companionData = transactionInfo.companionData
            ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_DATA_MISSING, "Missing organization uuid")
        val organization = getUuidFromCompanionData(companionData)
        val wallet = walletService.createOrganizationWallet(organization, signedTransaction)
        return wallet.activationData
    }

    private fun createProjectWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val companionData = transactionInfo.companionData
            ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_DATA_MISSING, "Missing project uuid")
        val project = getUuidFromCompanionData(companionData)
        val wallet = walletService.createProjectWallet(project, signedTransaction)
        return wallet.activationData
    }

    private fun confirmMintTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val companionData = transactionInfo.companionData
            ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_DATA_MISSING, "Missing deposit id")
        val depositId = getIdFromCompanionData(companionData)
        val deposit = depositService.confirmMintTransaction(signedTransaction, depositId)
        return deposit.txHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing txHash for mint transaction")
    }

    private fun confirmApprovalTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getWithdrawIdFromTransactionInfo(transactionInfo)
        val withdraw = withdrawService.confirmApproval(signedTransaction, withdrawId)
        return withdraw.approvedTxHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing approvedTxHash for withdraw transaction")
    }

    private fun burnTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getWithdrawIdFromTransactionInfo(transactionInfo)
        val withdraw = withdrawService.burn(signedTransaction, withdrawId)
        return withdraw.burnedTxHash
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Missing burnedTxHash for withdraw transaction")
    }

    private fun getWithdrawIdFromTransactionInfo(transactionInfo: TransactionInfo): Int {
        val companionData = transactionInfo.companionData
            ?: throw ResourceNotFoundException(ErrorCode.TX_COMPANION_DATA_MISSING, "Missing withdraw id")
        return getIdFromCompanionData(companionData)
    }

    private fun getTransactionInfo(txId: Int) = transactionInfoService.findTransactionInfo(txId)
        ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Non existing transaction with id: $txId")

    private fun getIdFromCompanionData(companionData: String): Int =
        try {
            companionData.toInt()
        } catch (ex: NumberFormatException) {
            throw InternalException(ErrorCode.INT_INVALID_VALUE, "Companion data is not Int")
        }

    private fun getUuidFromCompanionData(companionData: String): UUID =
        try {
            UUID.fromString(companionData)
        } catch (ex: IllegalArgumentException) {
            throw InternalException(ErrorCode.INT_INVALID_VALUE, "Companion data is not UUID")
        }
}
