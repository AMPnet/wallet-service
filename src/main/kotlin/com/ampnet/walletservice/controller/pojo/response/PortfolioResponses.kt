package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.project.proto.ProjectResponse
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.service.pojo.ProjectWithInvestment

data class ProjectWithInvestmentResponse(val project: ProjectControllerResponse, val investment: Long) {
    constructor(projectWithInvestment: ProjectWithInvestment) : this(
        ProjectControllerResponse(projectWithInvestment.project),
        projectWithInvestment.investment
    )
}
data class PortfolioResponse(val portfolio: List<ProjectWithInvestmentResponse>)
data class ProjectWithInvestments(val project: ProjectControllerResponse, val transactions: List<BlockchainTransaction>) {
    constructor(project: ProjectResponse, transactions: List<BlockchainTransaction>) : this (
        ProjectControllerResponse(project),
        transactions
    )
}
