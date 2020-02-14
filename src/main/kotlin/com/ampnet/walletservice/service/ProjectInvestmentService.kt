package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.service.pojo.ProjectInvestmentRequest
import java.util.UUID

interface ProjectInvestmentService {
    fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo
    fun investInProject(signedTransaction: String): String
    fun generateCancelInvestmentsInProjectTransaction(projectUuid: UUID, userUuid: UUID): TransactionDataAndInfo
    fun cancelInvestmentsInProject(signedTransaction: String): String
}
