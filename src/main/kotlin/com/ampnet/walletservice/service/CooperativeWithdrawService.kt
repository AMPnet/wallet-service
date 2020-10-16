package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.pojo.request.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.response.WithdrawListServiceResponse
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CooperativeWithdrawService {
    fun getAllApproved(type: DepositWithdrawType, pageable: Pageable): WithdrawListServiceResponse
    fun getAllBurned(type: DepositWithdrawType, pageable: Pageable): WithdrawListServiceResponse
    fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
    fun burn(signedTransaction: String, withdrawId: Int): Withdraw
    fun addDocument(withdrawId: Int, request: DocumentSaveRequest): WithdrawServiceResponse
}
