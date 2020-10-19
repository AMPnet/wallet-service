package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.CooperativeWithdrawService
import com.ampnet.walletservice.service.StorageService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WalletService
import com.ampnet.walletservice.service.pojo.request.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.response.WithdrawListServiceResponse
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import com.ampnet.walletservice.service.pojo.response.WithdrawWithDataServiceResponse
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class CooperativeWithdrawServiceImpl(
    private val walletRepository: WalletRepository,
    private val withdrawRepository: WithdrawRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService,
    private val userService: UserService,
    private val projectService: ProjectService,
    private val walletService: WalletService
) : CooperativeWithdrawService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getAllApproved(type: DepositWithdrawType, pageable: Pageable): WithdrawListServiceResponse {
        val withdrawsPage = withdrawRepository.findAllApproved(type, pageable)
        return when (type) {
            DepositWithdrawType.USER -> getWithdrawWithUserListServiceResponse(withdrawsPage)
            DepositWithdrawType.PROJECT -> getWithdrawWithProjectListServiceResponse(withdrawsPage)
        }
    }

    @Transactional(readOnly = true)
    override fun getAllBurned(type: DepositWithdrawType, pageable: Pageable): WithdrawListServiceResponse {
        val withdrawsPage = withdrawRepository.findAllBurned(type, pageable)
        return when (type) {
            DepositWithdrawType.USER -> getWithdrawWithUserListServiceResponse(withdrawsPage)
            DepositWithdrawType.PROJECT -> getWithdrawWithProjectListServiceResponse(withdrawsPage)
        }
    }

    @Transactional
    override fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        logger.info { "Generating Burn transaction for withdraw: $withdraw" }
        validateWithdrawForBurn(withdraw)
        val ownerWallet = ServiceUtils.getWalletHash(withdraw.ownerUuid, walletRepository)
        val data = blockchainService.generateBurnTransaction(ownerWallet)
        val info = transactionInfoService.createBurnTransaction(withdraw.amount, user, withdraw.id)
        withdraw.burnedBy = user
        logger.info { "Burned withdraw: $withdraw" }
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun burn(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        validateWithdrawForBurn(withdraw)
        logger.info { "Burning Withdraw: $withdraw" }
        val burnedTxHash = blockchainService.postTransaction(signedTransaction)
        withdraw.burnedTxHash = burnedTxHash
        withdraw.burnedAt = ZonedDateTime.now()
        logger.info { "Burned Withdraw: $withdraw" }
        mailService.sendWithdrawInfo(withdraw.ownerUuid, true)
        return withdraw
    }

    @Transactional
    override fun addDocument(withdrawId: Int, request: DocumentSaveRequest): WithdrawServiceResponse {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        logger.info { "Adding document for withdraw: $withdraw" }
        val document = storageService.saveDocument(request)
        withdraw.file = document
        logger.info { "Document added" }
        return WithdrawServiceResponse(withdraw, true)
    }

    override fun getById(id: Int): WithdrawWithDataServiceResponse? =
        ServiceUtils.wrapOptional(withdrawRepository.findById(id))?.let {
            getWithdrawWithData(it)
        }

    private fun validateWithdrawForBurn(withdraw: Withdraw) {
        if (withdraw.approvedTxHash == null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_NOT_APPROVED, "Withdraw must be approved")
        }
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Burned txHash: ${withdraw.burnedTxHash}")
        }
    }

    private fun getWithdrawWithUserListServiceResponse(withdrawsPage: Page<Withdraw>): WithdrawListServiceResponse {
        val withdraws = withdrawsPage.toList()
        val users = userService
            .getUsers(withdraws.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val withdrawWithUserList = withdraws.map { withdraw ->
            val walletHash = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
            val userResponse = users[withdraw.ownerUuid]
            WithdrawWithDataServiceResponse(withdraw, userResponse, null, walletHash)
        }
        return WithdrawListServiceResponse(withdrawWithUserList, withdrawsPage.number, withdrawsPage.totalPages)
    }

    private fun getWithdrawWithProjectListServiceResponse(withdrawsPage: Page<Withdraw>): WithdrawListServiceResponse {
        val withdraws = withdrawsPage.toList()
        val projects = projectService
            .getProjects(withdraws.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val users = userService
            .getUsers(withdraws.map { it.createdBy }.toSet())
            .associateBy { it.uuid }
        val withdrawWithProjectList = withdraws.map { withdraw ->
            val walletHash = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
            val projectResponse = projects[withdraw.ownerUuid]
            val createdBy = users[withdraw.createdBy]
            WithdrawWithDataServiceResponse(withdraw, createdBy, projectResponse, walletHash)
        }
        return WithdrawListServiceResponse(withdrawWithProjectList, withdrawsPage.number, withdrawsPage.totalPages)
    }

    private fun getWithdrawWithData(withdraw: Withdraw): WithdrawWithDataServiceResponse {
        return when (withdraw.type) {
            DepositWithdrawType.USER -> {
                val user = userService.getUsers(setOf(withdraw.ownerUuid)).firstOrNull()
                val walletHash = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
                WithdrawWithDataServiceResponse(withdraw, user, null, walletHash)
            }
            DepositWithdrawType.PROJECT -> {
                val user = userService.getUsers(setOf(withdraw.createdBy)).firstOrNull()
                val project = projectService.getProjects(setOf(withdraw.ownerUuid)).firstOrNull()
                val walletHash = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
                WithdrawWithDataServiceResponse(withdraw, user, project, walletHash)
            }
        }
    }
}
