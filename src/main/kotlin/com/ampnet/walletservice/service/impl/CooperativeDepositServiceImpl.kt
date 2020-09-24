package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
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
import com.ampnet.walletservice.service.pojo.GetDepositsServiceRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

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
        val deposit = getDepositForIdAndCoop(request.id, request.user.coop)
        // TODO: think about document reading restrictions
        val document = storageService.saveDocument(request.documentSaveRequest)
        logger.info { "Approving deposit: ${request.id} by user: ${request.user.uuid}" }
        return deposit.apply {
            approved = true
            approvedByUserUuid = request.user.uuid
            approvedAt = ZonedDateTime.now()
            amount = request.amount
            file = document
        }
    }

    @Transactional
    override fun decline(id: Int, user: UserPrincipal, comment: String): Deposit {
        val deposit = getDepositForIdAndCoop(id, user.coop)
        if (deposit.txHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_MINTED, "Cannot decline minted deposit")
        }
        logger.info { "Declining deposit: $id by user: $user" }
        val declined = Declined(comment, user.uuid)
        mailService.sendDepositInfo(deposit.ownerUuid, false)
        return deposit.apply {
            this.declined = declinedRepository.save(declined)
            approved = false
        }
    }

    @Transactional(readOnly = true)
    override fun getAllWithDocuments(request: GetDepositsServiceRequest, pageable: Pageable): Page<Deposit> {
        return depositRepository.findAllWithFile(request.approved, request.type, request.coop, pageable)
    }

    @Transactional(readOnly = true)
    override fun getUnsigned(type: DepositWithdrawType, coop: String, pageable: Pageable): Page<Deposit> {
        return depositRepository.findApprovedUnsignedWithFile(type, coop, pageable)
    }

    @Transactional(readOnly = true)
    override fun findByReference(reference: String, coop: String): Deposit? {
        return ServiceUtils.wrapOptional(depositRepository.findByReferenceAndCoop(reference, coop))
    }

    @Transactional
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
    override fun confirmMintTransaction(signedTransaction: String, depositId: Int, coop: String): Deposit {
        logger.info { "Confirming mint transaction for deposit: $depositId" }
        val deposit = getDepositForIdAndCoop(depositId, coop)
        validateDepositForMintTransaction(deposit)
        val txHash = blockchainService.postTransaction(signedTransaction)
        deposit.txHash = txHash
        mailService.sendDepositInfo(deposit.ownerUuid, true)
        return deposit
    }

    @Transactional(readOnly = true)
    override fun countUsersWithApprovedDeposit(coop: String): Int {
        return depositRepository.countUsersWithApprovedDeposit(coop)
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

    private fun getDepositForIdAndCoop(depositId: Int, coop: String): Deposit {
        return depositRepository.findByIdAndCoop(depositId, coop).orElseThrow {
            throw ResourceNotFoundException(
                ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit for id: $depositId and cooperative with id: $coop"
            )
        }
    }
}
