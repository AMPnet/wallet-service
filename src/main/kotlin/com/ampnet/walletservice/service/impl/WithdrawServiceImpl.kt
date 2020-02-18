package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.StorageService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WithdrawService
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.WithdrawCreateServiceRequest
import java.time.ZonedDateTime
import java.util.UUID
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WithdrawServiceImpl(
    private val walletRepository: WalletRepository,
    private val withdrawRepository: WithdrawRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService,
    private val projectService: ProjectService
) : WithdrawService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getPendingForUser(user: UUID): Withdraw? {
        return withdrawRepository.findByOwnerUuid(user).find { it.approvedTxHash == null }
    }

    @Transactional(readOnly = true)
    override fun getAllApproved(type: WalletType, pageable: Pageable): Page<Withdraw> {
        return withdrawRepository.findAllApproved(type, pageable)
    }

    @Transactional(readOnly = true)
    override fun getAllBurned(type: WalletType, pageable: Pageable): Page<Withdraw> {
        return withdrawRepository.findAllBurned(type, pageable)
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
    override fun deleteWithdraw(withdrawId: Int) {
        val withdraw = getWithdraw(withdrawId)
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Cannot delete burned Withdraw")
        }
        // TODO: check if the user is the owner of the withdraw
        logger.info { "Deleting Withdraw with id: $withdraw" }
        withdrawRepository.delete(withdraw)
        mailService.sendWithdrawInfo(withdraw.ownerUuid, false)
    }

    @Transactional
    override fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = getWithdraw(withdrawId)
        if (withdraw.ownerUuid != user) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_MISSING, "Withdraw does not belong to this user")
        }
        validateWithdrawForApproval(withdraw)
        val userWallet = ServiceUtils.getWalletHash(withdraw.ownerUuid, walletRepository)
        val data = blockchainService.generateApproveBurnTransaction(userWallet, withdraw.amount)
        val info = transactionInfoService.createApprovalTransaction(withdraw.amount, user, withdraw.id)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForApproval(withdraw)
        logger.info { "Approving Withdraw: $withdraw" }
        val approvalTxHash = blockchainService.postTransaction(signedTransaction)
        withdraw.approvedTxHash = approvalTxHash
        withdraw.approvedAt = ZonedDateTime.now()
        logger.info { "Approved Withdraw: $withdraw" }
        return withdrawRepository.save(withdraw)
    }

    @Transactional
    override fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForBurn(withdraw)
        val userWallet = ServiceUtils.getWalletHash(withdraw.ownerUuid, walletRepository)
        val data = blockchainService.generateBurnTransaction(userWallet)
        val info = transactionInfoService.createBurnTransaction(withdraw.amount, user, withdraw.id)
        withdraw.burnedBy = user
        withdrawRepository.save(withdraw)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun burn(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForBurn(withdraw)
        logger.info { "Burning Withdraw: $withdraw" }
        val burnedTxHash = blockchainService.postTransaction(signedTransaction)
        withdraw.burnedTxHash = burnedTxHash
        withdraw.burnedAt = ZonedDateTime.now()
        withdrawRepository.save(withdraw)
        logger.info { "Burned Withdraw: $withdraw" }
        mailService.sendWithdrawInfo(withdraw.ownerUuid, true)
        return withdraw
    }

    @Transactional
    override fun addDocument(withdrawId: Int, request: DocumentSaveRequest): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        val document = storageService.saveDocument(request)
        withdraw.file = document
        return withdrawRepository.save(withdraw)
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

    private fun validateWithdrawForApproval(withdraw: Withdraw) {
        if (withdraw.approvedTxHash != null) {
            throw InvalidRequestException(
                    ErrorCode.WALLET_WITHDRAW_APPROVED, "Approved txHash: ${withdraw.approvedTxHash}")
        }
    }

    private fun validateWithdrawForBurn(withdraw: Withdraw) {
        if (withdraw.approvedTxHash == null) {
            throw InvalidRequestException(
                    ErrorCode.WALLET_WITHDRAW_NOT_APPROVED, "Withdraw must be approved")
        }
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Burned txHash: ${withdraw.burnedTxHash}")
        }
    }

    private fun getWithdraw(withdrawId: Int): Withdraw {
        return withdrawRepository.findById(withdrawId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_WITHDRAW_MISSING, "Missing withdraw with id: $withdrawId")
        }
    }
}
