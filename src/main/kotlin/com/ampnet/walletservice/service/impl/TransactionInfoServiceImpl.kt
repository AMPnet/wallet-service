package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.persistence.repository.TransactionInfoRepository
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.CreateTransactionRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionInfoServiceImpl(
    private val transactionInfoRepository: TransactionInfoRepository
) : TransactionInfoService {

    private val activateWallet = "Wallet Activation"
    private val activateWalletDescription = "You are signing transaction to activate wallet type: %s"
    private val createOrgTitle = "Create Organization"
    private val createOrgDescription = "You are signing transaction to create organization: %s"
    private val createProjectTitle = "Create Project"
    private val createProjectDescription = "You are signing transaction to create project: %s"
    private val investTitle = "Invest"
    private val investDescription = "You are signing transaction to investment to project: %s with amount %.2f"
    private val mintTitle = "Mint"
    private val mintDescription = "You are singing mint transaction for wallet: %s"
    private val approvalTitle = "Approval"
    private val approvalDescription = "You are singing approval transaction to burn amount: %d"
    private val burnTitle = "Approval"
    private val burnDescription = "You are singing burn transaction for amount: %d"

    @Transactional
    override fun activateWalletTransaction(walletUuid: UUID, type: WalletType, userUuid: UUID): TransactionInfo {
        val description = activateWalletDescription.format(type.name)
        val request = CreateTransactionRequest(
            TransactionType.WALLET_ACTIVATE, activateWallet, description, userUuid, walletUuid.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createOrgTransaction(organization: UUID, organizationName: String, userUuid: UUID): TransactionInfo {
        val description = createOrgDescription.format(organizationName)
        val request = CreateTransactionRequest(
                TransactionType.CREATE_ORG, createOrgTitle, description, userUuid, organization.toString())
        return createTransaction(request)
    }

    @Transactional
    override fun createProjectTransaction(project: UUID, projectName: String, userUuid: UUID): TransactionInfo {
        val description = createProjectDescription.format(projectName)
        val request = CreateTransactionRequest(
                TransactionType.CREATE_PROJECT, createProjectTitle, description, userUuid, project.toString())
        return createTransaction(request)
    }

    @Transactional
    @Suppress("MagicNumber")
    override fun createInvestTransaction(projectName: String, amount: Long, userUuid: UUID): TransactionInfo {
        val description = investDescription.format(projectName, amount.toDouble().div(100))
        val request = CreateTransactionRequest(
                TransactionType.INVEST, investTitle, description, userUuid)
        return createTransaction(request)
    }

    @Transactional
    override fun createMintTransaction(request: MintServiceRequest, receivingWallet: String): TransactionInfo {
        val description = mintDescription.format(receivingWallet)
        val txRequest = CreateTransactionRequest(
                TransactionType.MINT, mintTitle, description, request.byUser, request.depositId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun createApprovalTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo {
        val description = approvalDescription.format(amount)
        val txRequest = CreateTransactionRequest(
                TransactionType.BURN_APPROVAL, approvalTitle, description, userUuid, withdrawId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun createBurnTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo {
        val description = burnDescription.format(amount)
        val txRequest = CreateTransactionRequest(
            TransactionType.BURN, burnTitle, description, userUuid, withdrawId.toString())
        return createTransaction(txRequest)
    }

    @Transactional
    override fun deleteTransaction(id: Int) = transactionInfoRepository.deleteById(id)

    @Transactional(readOnly = true)
    override fun findTransactionInfo(id: Int): TransactionInfo? =
            ServiceUtils.wrapOptional(transactionInfoRepository.findById(id))

    private fun createTransaction(request: CreateTransactionRequest): TransactionInfo {
        val transaction = TransactionInfo::class.java.getDeclaredConstructor().newInstance().apply {
            type = request.type
            title = request.title
            description = request.description
            userUuid = request.userUuid
            companionData = request.companionData
        }
        return transactionInfoRepository.save(transaction)
    }
}
