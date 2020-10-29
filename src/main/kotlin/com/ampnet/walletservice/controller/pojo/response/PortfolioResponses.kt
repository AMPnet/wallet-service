package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import com.ampnet.walletservice.service.pojo.response.ProjectWithInvestment

data class ProjectWithInvestmentResponse(val project: ProjectServiceResponse, val investment: Long) {
    constructor(projectWithInvestment: ProjectWithInvestment) : this(
        projectWithInvestment.project,
        projectWithInvestment.investment
    )
}

data class PortfolioResponse(val portfolio: List<ProjectWithInvestmentResponse>)
data class ProjectWithInvestments(val project: ProjectServiceResponse, val transactions: List<BlockchainTransaction>)

data class BlockchainTransactionsResponse(val transactions: List<BlockchainTransaction>)
