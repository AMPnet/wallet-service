package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.persistence.model.Declined
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.repository.DeclinedRepository
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.CooperativeDepositService
import com.ampnet.walletservice.service.StorageService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
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
    private val mailService: MailService
) : CooperativeDepositService {

    companion object : KLogging()

    @Transactional
    override fun approve(request: ApproveDepositRequest): Deposit {
        val deposit = getDepositForId(request.id)
        // TODO: think about document reading restrictions
        val document = storageService.saveDocument(request.documentSaveRequest)
        logger.info { "Approving deposit: ${request.id} by user: ${request.user}" }

        deposit.approved = true
        deposit.approvedByUserUuid = request.user
        deposit.approvedAt = ZonedDateTime.now()
        deposit.amount = request.amount
        deposit.file = document
        return depositRepository.save(deposit)
    }

    @Transactional
    override fun decline(id: Int, user: UUID, comment: String): Deposit {
        val deposit = getDepositForId(id)
        if (deposit.txHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_MINTED, "Cannot decline minted deposit")
        }
        logger.info { "Declining deposit: $id by user: $user" }
        val declined = Declined(comment, user)
        deposit.declined = declinedRepository.save(declined)
        deposit.approved = false
        mailService.sendDepositInfo(deposit.ownerUuid, false)
        return depositRepository.save(deposit)
    }

    @Transactional(readOnly = true)
    override fun getAllWithDocuments(approved: Boolean, type: DepositWithdrawType, pageable: Pageable): Page<Deposit> {
        return depositRepository.findAllWithFile(approved, type, pageable)
    }

    @Transactional(readOnly = true)
    override fun getUnsigned(type: DepositWithdrawType, pageable: Pageable): Page<Deposit> {
        return depositRepository.findApprovedUnsignedWithFile(type, pageable)
    }

    @Transactional(readOnly = true)
    override fun findByReference(reference: String): Deposit? {
        return ServiceUtils.wrapOptional(depositRepository.findByReference(reference))
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
        return depositRepository.save(deposit)
    }

    @Transactional(readOnly = true)
    override fun countUsersWithApprovedDeposit(): Int {
        return depositRepository.countUsersWithApprovedDeposit()
    }

    private fun validateDepositForMintTransaction(deposit: Deposit) {
        if (deposit.approved.not()) {
            throw InvalidRequestException(
                ErrorCode.WALLET_DEPOSIT_NOT_APPROVED,
                "Deposit: ${deposit.id} is not approved"
            )
        }
        if (deposit.txHash != null) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_MINTED, "Mint txHash: ${deposit.txHash}")
        }
    }

    private fun getDepositForId(depositId: Int): Deposit {
        return depositRepository.findById(depositId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit: $depositId")
        }
    }
}
