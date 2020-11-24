package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.pojo.request.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.response.WithdrawListServiceResponse
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import com.ampnet.walletservice.service.pojo.response.WithdrawWithDataServiceResponse
import org.springframework.data.domain.Pageable

interface CooperativeWithdrawService {
    fun getAllApproved(coop: String, type: DepositWithdrawType?, pageable: Pageable): WithdrawListServiceResponse
    fun getAllBurned(coop: String, type: DepositWithdrawType?, pageable: Pageable): WithdrawListServiceResponse
    fun generateBurnTransaction(withdrawId: Int, user: UserPrincipal): TransactionDataAndInfo
    fun burn(signedTransaction: String, withdrawId: Int): Withdraw
    fun addDocument(withdrawId: Int, request: DocumentSaveRequest): WithdrawServiceResponse
    fun getById(id: Int): WithdrawWithDataServiceResponse?
    fun getPending(coop: String, type: DepositWithdrawType?, pageable: Pageable): WithdrawListServiceResponse
}
