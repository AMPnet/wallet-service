package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.pojo.request.WithdrawCreateServiceRequest
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import java.util.UUID

interface WithdrawService {
    fun getPendingForOwner(user: UUID): WithdrawServiceResponse?
    fun getPendingForProject(project: UUID, user: UUID): WithdrawServiceResponse?
    fun createWithdraw(request: WithdrawCreateServiceRequest): WithdrawServiceResponse
    fun deleteWithdraw(withdrawId: Int, user: UUID)
    fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
    fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw
}
