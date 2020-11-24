package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
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

@Service
@Suppress("TooManyFunctions")
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
    override fun getAllApproved(
        coop: String,
        type: DepositWithdrawType?,
        pageable: Pageable
    ): WithdrawListServiceResponse =
        when (type) {
            DepositWithdrawType.USER -> {
                val userWithdraws = withdrawRepository.findAllApprovedByType(coop, type, pageable)
                getWithdrawWithUserListServiceResponse(userWithdraws)
            }
            DepositWithdrawType.PROJECT -> {
                val projectWithdraws = withdrawRepository.findAllApprovedByType(coop, type, pageable)
                getWithdrawWithProjectListServiceResponse(projectWithdraws)
            }
            else -> generateWithdrawListResponse(withdrawRepository.findAllApproved(coop, pageable))
        }

    @Transactional(readOnly = true)
    override fun getAllBurned(
        coop: String,
        type: DepositWithdrawType?,
        pageable: Pageable
    ): WithdrawListServiceResponse =
        when (type) {
            DepositWithdrawType.USER -> {
                val userWithdraws = withdrawRepository.findAllBurnedByType(coop, type, pageable)
                getWithdrawWithUserListServiceResponse(userWithdraws)
            }
            DepositWithdrawType.PROJECT -> {
                val projectWithdraws = withdrawRepository.findAllBurnedByType(coop, type, pageable)
                getWithdrawWithProjectListServiceResponse(projectWithdraws)
            }
            else -> generateWithdrawListResponse(withdrawRepository.findAllBurned(coop, pageable))
        }

    @Transactional
    override fun generateBurnTransaction(withdrawId: Int, user: UserPrincipal): TransactionDataAndInfo {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        logger.info { "Generating Burn transaction for withdraw: $withdraw" }
        validateWithdrawForBurn(withdraw)
        val ownerWallet = ServiceUtils.getWalletHash(withdraw.ownerUuid, walletRepository)
        val data = blockchainService.generateBurnTransaction(ownerWallet)
        val info = transactionInfoService.createBurnTransaction(withdraw.amount, user, withdraw.id)
        withdraw.burnedBy = user.uuid
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
        val withdraw = ServiceUtils.getWithdrawForIdAndCoop(withdrawId, request.user.coop, withdrawRepository)
        logger.info { "Adding document for withdraw: $withdraw" }
        val document = storageService.saveDocument(request)
        withdraw.file = document
        logger.info { "Document added" }
        return WithdrawServiceResponse(withdraw, true)
    }

    @Transactional(readOnly = true)
    override fun getById(id: Int): WithdrawWithDataServiceResponse? =
        ServiceUtils.wrapOptional(withdrawRepository.findById(id))?.let {
            getWithdrawWithData(it)
        }

    @Transactional(readOnly = true)
    override fun getPending(coop: String, type: DepositWithdrawType?, pageable: Pageable): WithdrawListServiceResponse =
        generateWithdrawListResponse(withdrawRepository.findAllPending(coop, type, pageable))

    private fun validateWithdrawForBurn(withdraw: Withdraw) {
        if (withdraw.approvedTxHash == null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_NOT_APPROVED, "Withdraw must be approved")
        }
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Burned txHash: ${withdraw.burnedTxHash}")
        }
    }

    private fun getWithdrawWithUserListServiceResponse(withdrawsPage: Page<Withdraw>): WithdrawListServiceResponse {
        val withdrawWithUserList = getWithdrawsWithUser(withdrawsPage.toList())
        return WithdrawListServiceResponse(withdrawWithUserList, withdrawsPage.number, withdrawsPage.totalPages)
    }

    private fun getWithdrawsWithUser(withdraws: List<Withdraw>): List<WithdrawWithDataServiceResponse> {
        val users = userService
            .getUsers(withdraws.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        return withdraws.map { withdraw ->
            val walletHash = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
            val userResponse = users[withdraw.ownerUuid]
            WithdrawWithDataServiceResponse(withdraw, userResponse, null, walletHash)
        }
    }

    private fun getWithdrawWithProjectListServiceResponse(withdrawsPage: Page<Withdraw>): WithdrawListServiceResponse {
        val withdrawWithProjectList = getWithdrawsWithProject(withdrawsPage.toList())
        return WithdrawListServiceResponse(withdrawWithProjectList, withdrawsPage.number, withdrawsPage.totalPages)
    }

    private fun getWithdrawsWithProject(withdraws: List<Withdraw>): List<WithdrawWithDataServiceResponse> {
        val projects = projectService
            .getProjects(withdraws.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val users = userService
            .getUsers(withdraws.map { it.createdBy }.toSet())
            .associateBy { it.uuid }
        return withdraws.map { withdraw ->
            val walletHash = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
            val projectResponse = projects[withdraw.ownerUuid]
            val createdBy = users[withdraw.createdBy]
            WithdrawWithDataServiceResponse(withdraw, createdBy, projectResponse, walletHash)
        }
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

    private fun generateWithdrawListResponse(withdraws: Page<Withdraw>): WithdrawListServiceResponse {
        val withdrawsList = withdraws.toList()
        val userWithdraws = withdrawsList.filter { it.type == DepositWithdrawType.USER }
        val projectWithdraws = withdrawsList.filter { it.type == DepositWithdrawType.PROJECT }
        val allWithdraws =
            getWithdrawsWithUser(userWithdraws) + getWithdrawsWithProject(projectWithdraws)
        return WithdrawListServiceResponse(allWithdraws, withdraws.number, withdraws.totalPages)
    }
}
