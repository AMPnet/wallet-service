package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.service.pojo.ProjectInvestmentRequest

interface ProjectInvestmentService {
    fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo
    fun investInProject(signedTransaction: String): String
}
