package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.DepositService
import com.ampnet.walletservice.service.pojo.request.DepositCreateServiceRequest
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DepositServiceImpl(
    private val walletRepository: WalletRepository,
    private val depositRepository: DepositRepository,
    private val projectService: ProjectService
) : DepositService {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        private const val DEPOSIT_REFERENCE_LENGTH = 8
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, ResourceAlreadyExistsException::class, InvalidRequestException::class)
    override fun create(request: DepositCreateServiceRequest): DepositServiceResponse {
        validateOwnerHasWallet(request.owner)
        validateOwnerDoesNotHavePendingDeposit(request)
        if (request.type == DepositWithdrawType.PROJECT) {
            val projectResponse = projectService.getProject(request.owner)
            ServiceUtils.validateUserIsProjectOwner(request.createdBy.uuid, projectResponse)
        }
        val reference = generateUniqueReferenceForCoop(request.createdBy.coop)
        val deposit = Deposit(
            request.owner, reference, request.amount,
            request.createdBy.uuid, request.type, request.createdBy.coop
        )
        depositRepository.save(deposit)
        logger.debug {
            "Created Deposit for owner: ${request.owner} with amount: ${request.amount} by user: ${request.createdBy}"
        }
        return DepositServiceResponse(deposit)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun delete(id: Int, user: UUID) {
        val deposit = depositRepository.findById(id).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit: $id")
        }
        validateUserCanEditDeposit(deposit, user)
        if (deposit.txHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_MINTED, "Cannot delete minted deposit")
        }
        logger.debug { "Deleting deposit: $deposit" }
        depositRepository.delete(deposit)
    }

    @Transactional(readOnly = true)
    override fun getPendingForUser(user: UUID): DepositServiceResponse? {
        val deposit = depositRepository.findByOwnerUuidUnsigned(user).firstOrNull()
        return deposit?.let { DepositServiceResponse(it, true) }
    }

    @Transactional(readOnly = true)
    @Throws(InvalidRequestException::class)
    override fun getPendingForProject(project: UUID, user: UUID): DepositServiceResponse? {
        val deposit = depositRepository.findByOwnerUuidUnsigned(project).firstOrNull()
        return deposit?.let {
            validateUserCanEditDeposit(it, user)
            DepositServiceResponse(it, true)
        }
    }

    override fun getDepositForUserByTxHash(txHash: String?, user: UUID): List<DepositServiceResponse> {
        if (txHash != null) {
            val deposit = ServiceUtils.wrapOptional(depositRepository.findByTxHashAndOwnerUuid(txHash, user))
            return if (deposit == null) {
                listOf()
            } else {
                listOf(DepositServiceResponse(deposit, true))
            }
        }
        return depositRepository.findAllByOwnerUuid(user).map { DepositServiceResponse(it, true) }
    }

    private fun validateOwnerHasWallet(owner: UUID) {
        walletRepository.findByOwner(owner).orElseThrow {
            ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Missing wallet for owner: $owner")
        }
    }

    private fun validateUserCanEditDeposit(deposit: Deposit, user: UUID) {
        if (deposit.createdBy != user)
            throw InvalidRequestException(ErrorCode.USER_MISSING_PRIVILEGE, "Deposit does not belong to this user")
    }

    private fun validateOwnerDoesNotHavePendingDeposit(request: DepositCreateServiceRequest) {
        val pendingDeposit = when (request.type) {
            DepositWithdrawType.USER -> getPendingForUser(request.owner)
            DepositWithdrawType.PROJECT -> getPendingForProject(request.owner, request.createdBy.uuid)
        }
        if (pendingDeposit != null)
            throw ResourceAlreadyExistsException(
                ErrorCode.WALLET_DEPOSIT_EXISTS, "Check your unapproved deposit: ${pendingDeposit.id}"
            )
    }

    private fun generateDepositReference(): String = (1..DEPOSIT_REFERENCE_LENGTH)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    private fun generateUniqueReferenceForCoop(coop: String): String {
        lateinit var reference: String
        do {
            reference = generateDepositReference()
        } while (ServiceUtils.wrapOptional(depositRepository.findByCoopAndReference(coop, reference)) != null)
        return reference
    }
}
