package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.GrpcHandledException
import com.ampnet.walletservice.exception.InternalException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Deposit
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

@Service
@Suppress("TooManyFunctions")
class CooperativeDepositServiceImpl(
    private val walletRepository: WalletRepository,
    private val depositRepository: DepositRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService,
    private val userService: UserService,
    private val projectService: ProjectService
) : CooperativeDepositService {

    companion object : KLogging()

    @Transactional
    @Throws(ResourceNotFoundException::class, InternalException::class)
    override fun approve(request: ApproveDepositRequest): DepositServiceResponse {
        val deposit = getDepositForIdAndCoop(request.id, request.user.coop)
        // TODO: think about document reading restrictions
        val document = storageService.saveDocument(request.documentSaveRequest)
        logger.info { "Approving deposit: ${request.id} by user: ${request.user}" }

        deposit.approvedByUserUuid = request.user.uuid
        deposit.approvedAt = ZonedDateTime.now()
        deposit.amount = request.amount
        deposit.file = document

        return DepositServiceResponse(deposit, true)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun decline(id: Int, user: UserPrincipal) {
        val deposit = getDepositForIdAndCoop(id, user.coop)
        if (deposit.txHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_MINTED, "Cannot decline minted deposit")
        }
        logger.info { "Declining deposit: $id by user: $user" }
        depositRepository.delete(deposit)
        mailService.sendDepositInfo(deposit.ownerUuid, false)
    }

    @Transactional(readOnly = true)
    override fun getApprovedWithDocuments(
        coop: String,
        type: DepositWithdrawType?,
        pageable: Pageable
    ): DepositListServiceResponse =
        getDepositWithDataServiceResponse(depositRepository.findAllApprovedWithFile(coop, type, pageable), true)

    @Transactional(readOnly = true)
    override fun getUnapproved(
        coop: String,
        type: DepositWithdrawType?,
        pageable: Pageable
    ): DepositListServiceResponse =
        getDepositWithDataServiceResponse(depositRepository.findAllUnapproved(coop, type, pageable), false)

    @Transactional(readOnly = true)
    override fun findByReference(coop: String, reference: String): DepositWithDataServiceResponse? =
        ServiceUtils.wrapOptional(depositRepository.findByCoopAndReference(coop, reference))?.let {
            getDepositWithData(it)
        }

    @Transactional
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        ResourceAlreadyExistsException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo {
        logger.info { "Generating mint transaction for deposit: ${request.depositId} by user: ${request.byUser}" }
        val deposit = getDepositForIdAndCoop(request.depositId, request.byUser.coop)
        validateDepositForMintTransaction(deposit)
        val amount = deposit.amount
        val receivingWallet = ServiceUtils.getWalletHash(deposit.ownerUuid, walletRepository)
        val data = blockchainService.generateMintTransaction(receivingWallet, amount)
        val info = transactionInfoService.createMintTransaction(request, receivingWallet)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        ResourceAlreadyExistsException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun confirmMintTransaction(coop: String, signedTransaction: String, depositId: Int): Deposit {
        logger.info { "Confirming mint transaction for deposit: $depositId" }
        val deposit = getDepositForIdAndCoop(depositId, coop)
        validateDepositForMintTransaction(deposit)
        val txHash = blockchainService.postTransaction(signedTransaction, deposit.coop)
        deposit.txHash = txHash
        mailService.sendDepositInfo(deposit.ownerUuid, true)
        return deposit
    }

    @Transactional(readOnly = true)
    override fun countUsersWithApprovedDeposit(coop: String): Int =
        depositRepository.countUsersWithApprovedDeposit(coop)

    @Transactional(readOnly = true)
    override fun getById(coop: String, id: Int): DepositWithDataServiceResponse? =
        ServiceUtils.wrapOptional(depositRepository.findByIdAndCoop(id, coop))?.let {
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

    private fun getDepositForIdAndCoop(depositId: Int, coop: String): Deposit {
        return depositRepository.findByIdAndCoop(depositId, coop).orElseThrow {
            throw ResourceNotFoundException(
                ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit for id: $depositId and cooperative with id: $coop"
            )
        }
    }

    private fun getDepositWithDataServiceResponse(
        depositsPage: Page<Deposit>,
        withDocuments: Boolean = false
    ): DepositListServiceResponse {
        val depositsList = depositsPage.toList()
        val userDeposits = depositsList.filter { it.type == DepositWithdrawType.USER }
        val projectDeposits = depositsList.filter { it.type == DepositWithdrawType.PROJECT }
        val allDeposits =
            getDepositsWithUser(userDeposits, withDocuments) + getDepositsWithProject(projectDeposits, withDocuments)
        return DepositListServiceResponse(allDeposits, depositsPage.number, depositsPage.totalPages)
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
