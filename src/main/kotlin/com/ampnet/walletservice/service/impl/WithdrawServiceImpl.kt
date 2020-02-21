package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InternalException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.ApproveProjectBurnTransactionRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WithdrawService
import com.ampnet.walletservice.service.pojo.WithdrawCreateServiceRequest
import java.time.ZonedDateTime
import java.util.UUID
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WithdrawServiceImpl(
    private val walletRepository: WalletRepository,
    private val withdrawRepository: WithdrawRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val mailService: MailService,
    private val projectService: ProjectService
) : WithdrawService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getPendingForOwner(user: UUID): Withdraw? {
        return withdrawRepository.findByOwnerUuid(user).find { it.approvedTxHash == null }
    }

    @Transactional(readOnly = true)
    override fun getPendingForProject(project: UUID, user: UUID): Withdraw? {
        val projectResponse = projectService.getProject(project)
        ServiceUtils.validateUserIsProjectOwner(user, projectResponse)
        return withdrawRepository.findByOwnerUuid(project).find { it.approvedTxHash == null }
    }

    @Transactional
    override fun createWithdraw(request: WithdrawCreateServiceRequest): Withdraw {
        validateOwnerDoesNotHavePendingWithdraw(request.owner)
        checkIfOwnerHasEnoughFunds(request.owner, request.amount)
        if (request.type == WalletType.PROJECT) {
            val projectResponse = projectService.getProject(request.owner)
            ServiceUtils.validateUserIsProjectOwner(request.createBy, projectResponse)
        }
        val withdraw = Withdraw(
            0, request.owner, request.amount, ZonedDateTime.now(), request.createBy, request.bankAccount,
            null, null, null, null, null, null,
            type = request.type
        )
        withdrawRepository.save(withdraw)
        mailService.sendWithdrawRequest(request.createBy, request.amount)
        logger.info {
            "Created Withdraw, type = ${request.type} for owner: ${request.owner} with amount: ${request.amount} " +
                "by user: ${request.createBy}"
        }
        return withdraw
    }

    @Transactional
    override fun deleteWithdraw(withdrawId: Int, user: UUID) {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        validateUserCanEditWithdraw(withdraw, user)
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Cannot delete burned Withdraw")
        }
        logger.info { "Deleting Withdraw with id: $withdraw" }
        withdrawRepository.delete(withdraw)
        mailService.sendWithdrawInfo(withdraw.ownerUuid, false)
    }

    @Transactional
    override fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        validateUserCanEditWithdraw(withdraw, user)
        ServiceUtils.validateWithdrawForApproval(withdraw)
        val data = getApprovalTransactionData(withdraw, user)
        val info = transactionInfoService.createApprovalTransaction(withdraw.amount, user, withdraw.id)
        return TransactionDataAndInfo(data, info)
    }

    private fun getApprovalTransactionData(withdraw: Withdraw, user: UUID): TransactionData {
        val userWallet = ServiceUtils.getWalletHash(user, walletRepository)
        return when (withdraw.type) {
            WalletType.USER -> {
                blockchainService.generateApproveBurnTransaction(userWallet, withdraw.amount)
            }
            WalletType.PROJECT -> {
                val projectWallet = ServiceUtils.getWalletHash(withdraw.ownerUuid, walletRepository)
                val request = ApproveProjectBurnTransactionRequest(projectWallet, withdraw.amount, userWallet)
                blockchainService.generateApproveProjectBurnTransaction(request)
            }
            WalletType.ORG -> {
                throw InternalException(ErrorCode.INT_INVALID_VALUE, "Organization withdraw approval not possible")
            }
        }
    }

    private fun validateUserCanEditWithdraw(withdraw: Withdraw, user: UUID) {
        when (withdraw.type) {
            WalletType.USER -> {
                if (withdraw.ownerUuid != user) {
                    throw InvalidRequestException(
                        ErrorCode.USER_MISSING_PRIVILEGE, "Withdraw does not belong to this user")
                }
            }
            WalletType.PROJECT -> {
                val projectResponse = projectService.getProject(withdraw.ownerUuid)
                ServiceUtils.validateUserIsProjectOwner(user, projectResponse)
            }
            WalletType.ORG -> throw InternalException(ErrorCode.INT_INVALID_VALUE, "Organization withdraw not possible")
        }
    }

    private fun checkIfOwnerHasEnoughFunds(owner: UUID, amount: Long) {
        val walletHash = ServiceUtils.getWalletHash(owner, walletRepository)
        val balance = blockchainService.getBalance(walletHash)
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
}
