package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CooperativeWithdrawService {
    fun getAllApproved(type: DepositWithdrawType, pageable: Pageable): Page<Withdraw>
    fun getAllBurned(type: DepositWithdrawType, pageable: Pageable): Page<Withdraw>
    fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
    fun burn(signedTransaction: String, withdrawId: Int): Withdraw
    fun addDocument(withdrawId: Int, request: DocumentSaveRequest): Withdraw
}
