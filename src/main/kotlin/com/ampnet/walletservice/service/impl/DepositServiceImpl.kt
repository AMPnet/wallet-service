package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.DepositService
import java.time.ZonedDateTime
import java.util.UUID
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DepositServiceImpl(
    private val walletRepository: WalletRepository,
    private val depositRepository: DepositRepository,
    private val mailService: MailService
) : DepositService {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        private const val DEPOSIT_REFERENCE_LENGTH = 8
    }

    @Transactional
    override fun create(user: UUID, amount: Long): Deposit {
        if (walletRepository.findByOwner(user).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User must have a wallet to create a Deposit")
        }
        val unapprovedDeposits = depositRepository.findByUserUuid(user).filter { it.approved.not() }
        if (unapprovedDeposits.isEmpty().not()) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_EXISTS,
                "Check your unapproved deposit: ${unapprovedDeposits.firstOrNull()?.id}")
        }

        val deposit = Deposit(0, user, generateDepositReference(), false, amount,
            null, null, null, null, ZonedDateTime.now())
        depositRepository.save(deposit)
        mailService.sendDepositRequest(user, amount)
        logger.debug { "Created Deposit for user: $user with amount: $amount" }
        return deposit
    }

    @Transactional(readOnly = true)
    override fun getPendingForUser(user: UUID): Deposit? {
        return depositRepository.findByUserUuid(user).find { it.approved.not() }
    }

    private fun generateDepositReference(): String = (1..DEPOSIT_REFERENCE_LENGTH)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
