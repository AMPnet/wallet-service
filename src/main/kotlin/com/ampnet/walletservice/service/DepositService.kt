package com.ampnet.walletservice.service

import com.ampnet.walletservice.service.pojo.request.DepositCreateServiceRequest
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
import java.util.UUID

interface DepositService {
    fun create(request: DepositCreateServiceRequest): DepositServiceResponse
    fun delete(id: Int, user: UUID)
    fun getPendingForUser(user: UUID): DepositServiceResponse?
    fun getPendingForProject(project: UUID, user: UUID): DepositServiceResponse?
    fun getDepositByTxHash(txHash: String?, user: UUID): List<DepositServiceResponse>
}
