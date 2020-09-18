package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CooperativeWithdrawService {
    fun getAllApproved(type: DepositWithdrawType, coop: String, pageable: Pageable): Page<Withdraw>
    fun getAllBurned(type: DepositWithdrawType, coop: String, pageable: Pageable): Page<Withdraw>
    fun generateBurnTransaction(withdrawId: Int, user: UserPrincipal): TransactionDataAndInfo
    fun burn(signedTransaction: String, withdrawId: Int): Withdraw
    fun addDocument(withdrawId: Int, request: DocumentSaveRequest): Withdraw
}
