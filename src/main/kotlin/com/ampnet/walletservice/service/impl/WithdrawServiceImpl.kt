package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.ApproveProjectBurnTransactionRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.BankAccountService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WithdrawService
import com.ampnet.walletservice.service.pojo.request.WithdrawCreateServiceRequest
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class WithdrawServiceImpl(
    private val walletRepository: WalletRepository,
    private val withdrawRepository: WithdrawRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val mailService: MailService,
    private val projectService: ProjectService,
    private val bankAccountService: BankAccountService
) : WithdrawService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getPendingForOwner(user: UUID): WithdrawServiceResponse? =
        withdrawRepository.findByOwnerUuid(user).find { it.approvedTxHash == null }?.let {
            WithdrawServiceResponse(it)
        }

    @Transactional(readOnly = true)
    override fun getPendingForProject(project: UUID, user: UUID): WithdrawServiceResponse? {
        val projectResponse = projectService.getProject(project)
        ServiceUtils.validateUserIsProjectOwner(user, projectResponse)
        return withdrawRepository.findByOwnerUuid(project).find { it.approvedTxHash == null }?.let {
            WithdrawServiceResponse(it)
        }
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class, InvalidRequestException::class)
    override fun createWithdraw(request: WithdrawCreateServiceRequest): WithdrawServiceResponse {
        bankAccountService.validateIban(request.bankAccount)
        validateOwnerDoesNotHavePendingWithdraw(request.owner)
        checkIfOwnerHasEnoughFunds(request.owner, request.amount)
        if (request.type == DepositWithdrawType.PROJECT) {
            val projectResponse = projectService.getProject(request.owner)
            ServiceUtils.validateUserIsProjectOwner(request.createBy.uuid, projectResponse)
        }
        val withdraw = Withdraw(
            0, request.owner, request.amount, ZonedDateTime.now(), request.createBy.uuid, request.bankAccount,
            null, null, null, null, null, null,
            type = request.type, coop = request.createBy.coop
        )
        withdrawRepository.save(withdraw)
        mailService.sendWithdrawRequest(request.createBy.uuid, request.amount)
        logger.info {
            "Created Withdraw, type = ${request.type} for owner: ${request.owner} with amount: ${request.amount} " +
                "by user: ${request.createBy}"
        }
        return WithdrawServiceResponse(withdraw)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun deleteWithdraw(withdrawId: Int, user: UUID) {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        validateWithdrawIsNotApproved(withdraw)
        validateUserCanEditWithdraw(withdraw, user)
        logger.info { "Deleting Withdraw with id: $withdraw" }
        withdrawRepository.delete(withdraw)
        mailService.sendWithdrawInfo(withdraw.ownerUuid, false)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun generateApprovalTransaction(withdrawId: Int, user: UserPrincipal): TransactionDataAndInfo {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        validateWithdrawIsNotApproved(withdraw)
        validateUserCanEditWithdraw(withdraw, user.uuid)
        val data = getApprovalTransactionData(withdraw, user.uuid)
        val info = transactionInfoService.createApprovalTransaction(withdraw.amount, user, withdraw.id)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        validateWithdrawIsNotApproved(withdraw)
        logger.info { "Approving Withdraw: $withdraw" }
        val approvalTxHash = blockchainService.postTransaction(signedTransaction, withdraw.coop)
        withdraw.approvedTxHash = approvalTxHash
        withdraw.approvedAt = ZonedDateTime.now()
        logger.info { "Approved Withdraw: $withdraw" }
        return withdraw
    }

    private fun getApprovalTransactionData(withdraw: Withdraw, user: UUID): TransactionData {
        val userWallet = ServiceUtils.getWalletHash(user, walletRepository)
        return when (withdraw.type) {
            DepositWithdrawType.USER -> {
                blockchainService.generateApproveBurnTransaction(userWallet, withdraw.amount)
            }
            DepositWithdrawType.PROJECT -> {
                val projectWallet = ServiceUtils.getWalletHash(withdraw.ownerUuid, walletRepository)
                val request = ApproveProjectBurnTransactionRequest(projectWallet, withdraw.amount, userWallet)
                blockchainService.generateApproveProjectBurnTransaction(request)
            }
        }
    }

    private fun validateUserCanEditWithdraw(withdraw: Withdraw, user: UUID) {
        when (withdraw.type) {
            DepositWithdrawType.USER -> {
                if (withdraw.ownerUuid != user) {
                    throw InvalidRequestException(
                        ErrorCode.USER_MISSING_PRIVILEGE, "Withdraw does not belong to this user"
                    )
                }
            }
            DepositWithdrawType.PROJECT -> {
                val projectResponse = projectService.getProject(withdraw.ownerUuid)
                ServiceUtils.validateUserIsProjectOwner(user, projectResponse)
            }
        }
    }

    private fun checkIfOwnerHasEnoughFunds(owner: UUID, amount: Long) {
        val walletHash = ServiceUtils.getWalletHash(owner, walletRepository)
        val balance = blockchainService.getBalance(walletHash) ?: 0
        if (amount > balance) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "Insufficient funds")
        }
    }

    private fun validateOwnerDoesNotHavePendingWithdraw(user: UUID) {
        withdrawRepository.findByOwnerUuid(user).forEach {
            if (it.approvedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unapproved Withdraw: ${it.id}")
            }
            if (it.approvedTxHash != null && it.burnedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unburned Withdraw: ${it.id}")
            }
        }
    }

    private fun validateWithdrawIsNotApproved(withdraw: Withdraw) {
        if (withdraw.approvedTxHash != null) {
            throw InvalidRequestException(
                ErrorCode.WALLET_WITHDRAW_APPROVED, "Approved txHash: ${withdraw.approvedTxHash}"
            )
        }
    }
}
