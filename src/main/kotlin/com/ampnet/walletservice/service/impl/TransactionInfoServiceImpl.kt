package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.controller.pojo.request.WalletTransferRequest
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.enums.TransferWalletType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.persistence.repository.TransactionInfoRepository
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.request.CreateTransactionRequest
import com.ampnet.walletservice.service.pojo.request.MintServiceRequest
import com.ampnet.walletservice.service.pojo.request.RevenuePayoutTxInfoRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionInfoServiceImpl(
    private val transactionInfoRepository: TransactionInfoRepository
) : TransactionInfoService {

    @Transactional
    override fun activateWalletTransaction(
        walletUuid: UUID,
        walletType: WalletType,
        user: UserPrincipal
    ): TransactionInfo {
        val type = TransactionType.WALLET_ACTIVATE
        val description = type.description.format(type.name)
        val request = CreateTransactionRequest(type, description, user, walletUuid.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createOrgTransaction(
        organization: UUID,
        organizationName: String,
        user: UserPrincipal
    ): TransactionInfo {
        val type = TransactionType.CREATE_ORG
        val description = type.description.format(organizationName)
        val request = CreateTransactionRequest(type, description, user, organization.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createProjectTransaction(project: UUID, projectName: String, user: UserPrincipal): TransactionInfo {
        val type = TransactionType.CREATE_PROJECT
        val description = type.description.format(projectName)
        val request = CreateTransactionRequest(type, description, user, project.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createInvestTransaction(projectName: String, amount: Long, user: UserPrincipal): TransactionInfo {
        val type = TransactionType.INVEST
        val description = type.description.format(projectName, amountInDecimal(amount))
        val request = CreateTransactionRequest(type, description, user)
        return createTransaction(request)
    }

    @Transactional
    override fun cancelInvestmentTransaction(projectName: String, user: UserPrincipal): TransactionInfo {
        val type = TransactionType.CANCEL_INVEST
        val description = type.description.format(projectName)
        val request = CreateTransactionRequest(type, description, user)
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
    override fun createApprovalTransaction(amount: Long, user: UserPrincipal, withdrawId: Int): TransactionInfo {
        val type = TransactionType.BURN_APPROVAL
        val description = type.description.format(amount)
        val txRequest = CreateTransactionRequest(type, description, user, withdrawId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun createBurnTransaction(amount: Long, user: UserPrincipal, withdrawId: Int): TransactionInfo {
        val type = TransactionType.BURN
        val description = type.description.format(amount)
        val txRequest = CreateTransactionRequest(type, description, user, withdrawId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun createRevenuePayoutTransaction(request: RevenuePayoutTxInfoRequest): TransactionInfo {
        val type = TransactionType.REVENUE_PAYOUT
        val description = type.description.format(amountInDecimal(request.amount), request.projectName)
        val transactionRequest = CreateTransactionRequest(
            type, description, request.user, request.revenuePayoutId.toString()
        )
        return createTransaction(transactionRequest)
    }

    @Transactional
    override fun createTransferOwnership(owner: UserPrincipal, request: WalletTransferRequest): TransactionInfo {
        val type = when (request.type) {
            TransferWalletType.TOKEN_ISSUER -> TransactionType.TRNSF_TOKEN_OWN
            TransferWalletType.PLATFORM_MANAGER -> TransactionType.TRNSF_PLTFRM_OWN
        }
        val description = type.description.format(request.userUuid.toString())
        val transactionRequest = CreateTransactionRequest(type, description, owner, request.userUuid.toString())
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
            request.user.uuid,
            request.companionData,
            request.user.coop
        )
        return transactionInfoRepository.save(transaction)
    }

    @Suppress("MagicNumber")
    private fun amountInDecimal(amount: Long): Double = amount.toDouble().div(100)
}
