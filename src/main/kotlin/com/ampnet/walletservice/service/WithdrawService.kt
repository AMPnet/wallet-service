package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.pojo.WithdrawCreateServiceRequest
import java.util.UUID

interface WithdrawService {
    fun getPendingForOwner(user: UUID): Withdraw?
    fun getPendingForProject(project: UUID, user: UUID): Withdraw?
    fun createWithdraw(request: WithdrawCreateServiceRequest): Withdraw
    fun deleteWithdraw(withdrawId: Int, user: UUID)
    fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
}
