package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.DepositService
import com.ampnet.walletservice.service.pojo.DepositCreateServiceRequest
import java.util.UUID
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DepositServiceImpl(
    private val walletRepository: WalletRepository,
    private val depositRepository: DepositRepository,
    private val mailService: MailService,
    private val projectService: ProjectService
) : DepositService {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        private const val DEPOSIT_REFERENCE_LENGTH = 8
    }

    @Transactional
    override fun create(request: DepositCreateServiceRequest): Deposit {
        validateOwnerDoesNotHavePendingDeposit(request.owner)
        if (walletRepository.findByOwner(request.owner).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Wallet needed to create a deposit")
        }
        if (request.type == DepositWithdrawType.PROJECT) {
            val projectResponse = projectService.getProject(request.owner)
            ServiceUtils.validateUserIsProjectOwner(request.createdBy, projectResponse)
        }

        val deposit = Deposit(
            request.owner, generateDepositReference(), request.amount, request.createdBy, request.type
        )
        depositRepository.save(deposit)
        mailService.sendDepositRequest(request.createdBy, request.amount)
        logger.debug {
            "Created Deposit for owner: ${request.owner} with amount: ${request.amount} by user: ${request.createdBy}"
        }
        return deposit
    }

    @Transactional
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
    override fun getPendingForUser(user: UUID): Deposit? {
        return depositRepository.findByOwnerUuid(user).find { it.approved.not() }
    }

    private fun validateUserCanEditDeposit(deposit: Deposit, user: UUID) {
        when (deposit.type) {
            DepositWithdrawType.USER -> {
                if (deposit.ownerUuid != user) {
                    throw InvalidRequestException(
                        ErrorCode.USER_MISSING_PRIVILEGE, "Deposit does not belong to this user")
                }
            }
            DepositWithdrawType.PROJECT -> {
                val projectResponse = projectService.getProject(deposit.ownerUuid)
                ServiceUtils.validateUserIsProjectOwner(user, projectResponse)
            }
        }
    }

    private fun validateOwnerDoesNotHavePendingDeposit(owner: UUID) {
        val unapprovedDeposits = depositRepository
            .findByOwnerUuid(owner)
            .filter { it.approved.not() }
        if (unapprovedDeposits.isEmpty().not()) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_EXISTS,
                "Check your unapproved deposit: ${unapprovedDeposits.firstOrNull()?.id}")
        }
    }

    private fun generateDepositReference(): String = (1..DEPOSIT_REFERENCE_LENGTH)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
