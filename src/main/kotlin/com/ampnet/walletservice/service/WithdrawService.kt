package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface WithdrawService {
    fun getPendingForUser(user: UUID): Withdraw?
    fun getAllApproved(pageable: Pageable): Page<Withdraw>
    fun getAllBurned(pageable: Pageable): Page<Withdraw>
    fun createWithdraw(user: UUID, amount: Long, bankAccount: String): Withdraw
    fun deleteWithdraw(withdrawId: Int)
    fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
    fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw
    fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
    fun burn(signedTransaction: String, withdrawId: Int): Withdraw
    fun addDocument(withdrawId: Int, request: DocumentSaveRequest): Withdraw
}
