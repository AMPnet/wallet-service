package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.DepositService
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.service.StorageService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class DepositServiceImpl(
    private val walletRepository: WalletRepository,
    private val depositRepository: DepositRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService
) : DepositService {

    companion object : KLogging()

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

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
            null, null, null, null, ZonedDateTime.now()
        )
        depositRepository.save(deposit)
        mailService.sendDepositRequest(user, amount)
        logger.debug { "Created Deposit for user: $user with amount: $amount" }
        return deposit
    }

    @Transactional
    override fun delete(id: Int) {
        val deposit = getDepositForId(id)
        if (deposit.txHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_MINTED, "Cannot delete minted deposit")
        }
        logger.info { "Deleting deposit: $deposit" }
        depositRepository.delete(deposit)
        mailService.sendDepositInfo(deposit.userUuid, false)
    }

    @Transactional
    override fun approve(request: ApproveDepositRequest): Deposit {
        val deposit = depositRepository.findById(request.id).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit: ${request.id}")
        }
        // TODO: think about document reading restrictions
        val document = storageService.saveDocument(request.documentSaveRequest)
        logger.info { "Approving deposit with id: ${request.id} by user: ${request.user}" }

        deposit.approved = true
        deposit.approvedByUserUuid = request.user
        deposit.approvedAt = ZonedDateTime.now()
        deposit.amount = request.amount
        deposit.file = document
        return depositRepository.save(deposit)
    }

    @Transactional(readOnly = true)
    override fun getAllWithDocuments(approved: Boolean): List<Deposit> {
        return depositRepository.findAllWithFile(approved)
    }

    @Transactional(readOnly = true)
    override fun findByReference(reference: String): Deposit? {
        return ServiceUtils.wrapOptional(depositRepository.findByReference(reference))
    }

    @Transactional(readOnly = true)
    override fun getPendingForUser(user: UUID): Deposit? {
        return depositRepository.findByUserUuid(user).find { it.approved.not() }
    }

    @Transactional
    override fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo {
        val deposit = getDepositForId(request.depositId)
        validateDepositForMintTransaction(deposit)
        val amount = deposit.amount
        val receivingWallet = ServiceUtils.getWalletHash(deposit.userUuid, walletRepository)
        val data = blockchainService.generateMintTransaction(receivingWallet, amount)
        val info = transactionInfoService.createMintTransaction(request, receivingWallet)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit {
        val deposit = getDepositForId(depositId)
        validateDepositForMintTransaction(deposit)
        val txHash = blockchainService.postTransaction(signedTransaction)
        deposit.txHash = txHash
        depositRepository.save(deposit)
        mailService.sendDepositInfo(deposit.userUuid, true)
        return deposit
    }

    private fun validateDepositForMintTransaction(deposit: Deposit) {
        if (deposit.approved.not()) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_NOT_APPROVED,
                "Deposit: ${deposit.id} is not approved")
        }
        if (deposit.txHash != null) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_MINTED, "Mint txHash: ${deposit.txHash}")
        }
    }

    private fun getDepositForId(depositId: Int): Deposit {
        return depositRepository.findById(depositId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING,
                "For mint transaction missing deposit: $depositId")
        }
    }

    private fun generateDepositReference(): String = (1..8)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
