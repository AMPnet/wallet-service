package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.persistence.repository.TransactionInfoRepository
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.CreateTransactionRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import com.ampnet.walletservice.service.pojo.RevenuePayoutTxInfo
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionInfoServiceImpl(
    private val transactionInfoRepository: TransactionInfoRepository
) : TransactionInfoService {

    @Transactional
    override fun activateWalletTransaction(walletUuid: UUID, walletType: WalletType, userUuid: UUID): TransactionInfo {
        val type = TransactionType.WALLET_ACTIVATE
        val description = type.description.format(type.name)
        val request = CreateTransactionRequest(type, description, userUuid, walletUuid.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createOrgTransaction(organization: UUID, organizationName: String, userUuid: UUID): TransactionInfo {
        val type = TransactionType.CREATE_ORG
        val description = type.description.format(organizationName)
        val request = CreateTransactionRequest(type, description, userUuid, organization.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createProjectTransaction(project: UUID, projectName: String, userUuid: UUID): TransactionInfo {
        val type = TransactionType.CREATE_PROJECT
        val description = type.description.format(projectName)
        val request = CreateTransactionRequest(type, description, userUuid, project.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createInvestTransaction(projectName: String, amount: Long, userUuid: UUID): TransactionInfo {
        val type = TransactionType.INVEST
        val description = type.description.format(projectName, amountInDecimal(amount))
        val request = CreateTransactionRequest(type, description, userUuid)
        return createTransaction(request)
    }

    @Transactional
    override fun cancelInvestmentTransaction(projectName: String, userUuid: UUID): TransactionInfo {
        val type = TransactionType.CANCEL_INVEST
        val description = type.description.format(projectName)
        val request = CreateTransactionRequest(type, description, userUuid)
        return createTransaction(request)
    }

    @Transactional
    override fun createMintTransaction(request: MintServiceRequest, receivingWallet: String): TransactionInfo {
        val type = TransactionType.MINT
        val description = type.description.format(receivingWallet)
        val txRequest = CreateTransactionRequest(type, description, request.byUser, request.depositId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun createApprovalTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo {
        val type = TransactionType.BURN_APPROVAL
        val description = type.description.format(amount)
        val txRequest = CreateTransactionRequest(type, description, userUuid, withdrawId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun createBurnTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo {
        val type = TransactionType.BURN
        val description = type.description.format(amount)
        val txRequest = CreateTransactionRequest(type, description, userUuid, withdrawId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun createRevenuePayoutTransaction(request: RevenuePayoutTxInfo): TransactionInfo {
        val type = TransactionType.REVENUE_PAYOUT
        val description = type.description.format(amountInDecimal(request.amount), request.projectName)
        val transactionRequest = CreateTransactionRequest(
            type, description, request.userUuid, request.revenuePayoutId.toString())
        return createTransaction(transactionRequest)
    }

    @Transactional
    override fun deleteTransaction(id: Int) = transactionInfoRepository.deleteById(id)

    @Transactional(readOnly = true)
    override fun findTransactionInfo(id: Int): TransactionInfo? =
        ServiceUtils.wrapOptional(transactionInfoRepository.findById(id))

    private fun createTransaction(request: CreateTransactionRequest): TransactionInfo {
        val transaction = TransactionInfo(
            0,
            request.type,
            request.description,
            request.userUuid,
            request.companionData
        )
        return transactionInfoRepository.save(transaction)
    }

    @Suppress("MagicNumber")
    private fun amountInDecimal(amount: Long): Double = amount.toDouble().div(100)
}
