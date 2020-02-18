package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.CooperativeWithdrawService
import com.ampnet.walletservice.service.StorageService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import java.time.ZonedDateTime
import java.util.UUID
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CooperativeWithdrawServiceImpl(
    private val walletRepository: WalletRepository,
    private val withdrawRepository: WithdrawRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService
) : CooperativeWithdrawService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getAllApproved(type: WalletType, pageable: Pageable): Page<Withdraw> {
        return withdrawRepository.findAllApproved(type, pageable)
    }

    @Transactional(readOnly = true)
    override fun getAllBurned(type: WalletType, pageable: Pageable): Page<Withdraw> {
        return withdrawRepository.findAllBurned(type, pageable)
    }

    @Transactional
    override fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        ServiceUtils.validateWithdrawForApproval(withdraw)
        logger.info { "Approving Withdraw: $withdraw" }
        val approvalTxHash = blockchainService.postTransaction(signedTransaction)
        withdraw.approvedTxHash = approvalTxHash
        withdraw.approvedAt = ZonedDateTime.now()
        logger.info { "Approved Withdraw: $withdraw" }
        return withdrawRepository.save(withdraw)
    }

    @Transactional
    override fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        validateWithdrawForBurn(withdraw)
        val userWallet = ServiceUtils.getWalletHash(withdraw.ownerUuid, walletRepository)
        val data = blockchainService.generateBurnTransaction(userWallet)
        val info = transactionInfoService.createBurnTransaction(withdraw.amount, user, withdraw.id)
        withdraw.burnedBy = user
        withdrawRepository.save(withdraw)
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
        withdrawRepository.save(withdraw)
        logger.info { "Burned Withdraw: $withdraw" }
        mailService.sendWithdrawInfo(withdraw.ownerUuid, true)
        return withdraw
    }

    @Transactional
    override fun addDocument(withdrawId: Int, request: DocumentSaveRequest): Withdraw {
        val withdraw = ServiceUtils.getWithdraw(withdrawId, withdrawRepository)
        val document = storageService.saveDocument(request)
        withdraw.file = document
        return withdrawRepository.save(withdraw)
    }

    private fun validateWithdrawForBurn(withdraw: Withdraw) {
        if (withdraw.approvedTxHash == null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_NOT_APPROVED, "Withdraw must be approved")
        }
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Burned txHash: ${withdraw.burnedTxHash}")
        }
    }
}
