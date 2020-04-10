package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.service.pojo.PortfolioStats
import com.ampnet.walletservice.service.pojo.ProjectWithInvestment
import java.util.UUID

interface PortfolioService {
    fun getPortfolio(user: UUID): List<ProjectWithInvestment>
    fun getPortfolioStats(user: UUID): PortfolioStats
    fun getInvestmentsInProject(user: UUID, project: UUID): List<BlockchainTransaction>
    fun getTransactions(user: UUID): List<BlockchainTransaction>
}
