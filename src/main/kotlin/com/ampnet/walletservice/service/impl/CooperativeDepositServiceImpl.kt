package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Declined
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.repository.DeclinedRepository
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.CooperativeDepositService
import com.ampnet.walletservice.service.StorageService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.request.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.request.MintServiceRequest
import com.ampnet.walletservice.service.pojo.response.DepositListServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositWithDataServiceResponse
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class CooperativeDepositServiceImpl(
    private val walletRepository: WalletRepository,
    private val depositRepository: DepositRepository,
    private val declinedRepository: DeclinedRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService,
    private val userService: UserService,
    private val projectService: ProjectService
) : CooperativeDepositService {

    companion object : KLogging()

    @Transactional
    override fun approve(request: ApproveDepositRequest): DepositServiceResponse {
        val deposit = getDepositForId(request.id)
        // TODO: think about document reading restrictions
        val document = storageService.saveDocument(request.documentSaveRequest)
        logger.info { "Approving deposit: ${request.id} by user: ${request.user}" }

        deposit.approvedByUserUuid = request.user
        deposit.approvedAt = ZonedDateTime.now()
        deposit.amount = request.amount
        deposit.file = document

        return DepositServiceResponse(deposit, true)
    }

    @Transactional
    override fun decline(id: Int, user: UUID, comment: String): DepositServiceResponse {
        val deposit = getDepositForId(id)
        if (deposit.txHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_MINTED, "Cannot decline minted deposit")
        }
        logger.info { "Declining deposit: $id by user: $user" }
        val declined = Declined(comment, user)
        deposit.declined = declinedRepository.save(declined)
        mailService.sendDepositInfo(deposit.ownerUuid, false)
        return DepositServiceResponse(deposit, true)
    }

    @Transactional(readOnly = true)
    override fun getApprovedWithDocuments(type: DepositWithdrawType?, pageable: Pageable): DepositListServiceResponse {
        var depositsPage: Page<Deposit>
        type?.let {
            depositsPage = depositRepository.findAllApprovedWithFileByType(type, pageable)
            return when (type) {
                DepositWithdrawType.USER -> getDepositWithUserListServiceResponse(depositsPage, true)
                DepositWithdrawType.PROJECT -> getDepositWithProjectListServiceResponse(depositsPage, true)
            }
        } ?: run {
            depositsPage = depositRepository.findAllApprovedWithFile(pageable)
            val userDeposits = depositsPage.toList().filter { it.type == DepositWithdrawType.USER }
            val projectDeposits = depositsPage.toList().filter { it.type == DepositWithdrawType.PROJECT }
            val allDeposits =
                getDepositsWithUser(userDeposits, true) + getDepositsWithProject(projectDeposits, true)
            return DepositListServiceResponse(allDeposits, depositsPage.number, depositsPage.totalPages)
        }
    }

    @Transactional(readOnly = true)
    override fun getUnapproved(type: DepositWithdrawType, pageable: Pageable): DepositListServiceResponse {
        val depositsPage = depositRepository.findAllUnapproved(type, pageable)
        return when (type) {
            DepositWithdrawType.USER -> getDepositWithUserListServiceResponse(depositsPage)
            DepositWithdrawType.PROJECT -> getDepositWithProjectListServiceResponse(depositsPage)
        }
    }

    @Transactional(readOnly = true)
    override fun findByReference(reference: String): DepositWithDataServiceResponse? =
        ServiceUtils.wrapOptional(depositRepository.findByReference(reference))?.let {
            getDepositWithData(it)
        }

    @Transactional
    override fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo {
        logger.info { "Generating mint transaction for deposit: ${request.depositId} by user: ${request.byUser}" }
        val deposit = getDepositForId(request.depositId)
        validateDepositForMintTransaction(deposit)
        val amount = deposit.amount
        val receivingWallet = ServiceUtils.getWalletHash(deposit.ownerUuid, walletRepository)
        val data = blockchainService.generateMintTransaction(receivingWallet, amount)
        val info = transactionInfoService.createMintTransaction(request, receivingWallet)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit {
        logger.info { "Confirming mint transaction for deposit: $depositId" }
        val deposit = getDepositForId(depositId)
        validateDepositForMintTransaction(deposit)
        val txHash = blockchainService.postTransaction(signedTransaction)
        deposit.txHash = txHash
        mailService.sendDepositInfo(deposit.ownerUuid, true)
        return deposit
    }

    @Transactional(readOnly = true)
    override fun countUsersWithApprovedDeposit(): Int = depositRepository.countUsersWithApprovedDeposit()

    @Transactional(readOnly = true)
    override fun getById(id: Int): DepositWithDataServiceResponse? =
        ServiceUtils.wrapOptional(depositRepository.findById(id))?.let {
            getDepositWithData(it)
        }

    private fun validateDepositForMintTransaction(deposit: Deposit) {
        if (deposit.approvedByUserUuid == null) {
            throw InvalidRequestException(
                ErrorCode.WALLET_DEPOSIT_NOT_APPROVED,
                "Deposit: ${deposit.id} is not approved"
            )
        }
        if (deposit.txHash != null) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_MINTED, "Mint txHash: ${deposit.txHash}")
        }
    }

    private fun getDepositForId(depositId: Int): Deposit =
        depositRepository.findById(depositId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit: $depositId")
        }

    private fun getDepositWithUserListServiceResponse(
        depositsPage: Page<Deposit>,
        withDocuments: Boolean = false
    ): DepositListServiceResponse {
        val depositsWithUser = getDepositsWithUser(depositsPage.toList(), withDocuments)
        return DepositListServiceResponse(depositsWithUser, depositsPage.number, depositsPage.totalPages)
    }

    private fun getDepositsWithUser(
        deposits: List<Deposit>,
        withDocuments: Boolean = false
    ): List<DepositWithDataServiceResponse> {
        val users = userService
            .getUsers(deposits.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        return deposits.map { deposit ->
            val user = users[deposit.ownerUuid]
            DepositWithDataServiceResponse(deposit, user, null, withDocuments)
        }
    }

    private fun getDepositsWithProject(
        deposits: List<Deposit>,
        withDocuments: Boolean = false
    ): List<DepositWithDataServiceResponse> {
        val projects = projectService
            .getProjects(deposits.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val users = userService
            .getUsers(deposits.map { it.createdBy }.toSet())
            .associateBy { it.uuid }
        return deposits.map { deposit ->
            val projectResponse = projects[deposit.ownerUuid]
            val createdBy = users[deposit.createdBy]
            DepositWithDataServiceResponse(deposit, createdBy, projectResponse, withDocuments)
        }
    }

    private fun getDepositWithProjectListServiceResponse(
        depositsPage: Page<Deposit>,
        withDocuments: Boolean = false
    ): DepositListServiceResponse {
        val depositsWithProject = getDepositsWithProject(depositsPage.toList(), withDocuments)
        return DepositListServiceResponse(depositsWithProject, depositsPage.number, depositsPage.totalPages)
    }

    private fun getDepositWithData(deposit: Deposit): DepositWithDataServiceResponse {
        return when (deposit.type) {
            DepositWithdrawType.USER -> {
                val user = userService.getUsers(setOf(deposit.ownerUuid)).firstOrNull()
                DepositWithDataServiceResponse(deposit, user, null)
            }
            DepositWithdrawType.PROJECT -> {
                val user = userService.getUsers(setOf(deposit.createdBy)).firstOrNull()
                val project = projectService.getProjects(setOf(deposit.ownerUuid)).firstOrNull()
                DepositWithDataServiceResponse(deposit, user, project)
            }
        }
    }
}
