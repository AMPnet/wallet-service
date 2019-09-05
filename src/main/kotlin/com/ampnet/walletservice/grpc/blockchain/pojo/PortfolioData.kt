package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.crowdfunding.proto.PortfolioResponse

data class PortfolioData(val projectTxHash: String, val amount: Long) {
    constructor(portfolioInvestment: PortfolioResponse.Investment) : this(
        portfolioInvestment.projectTxHash, portfolioInvestment.amount.toLong()
    )
}
data class Portfolio(val data: List<PortfolioData>)
