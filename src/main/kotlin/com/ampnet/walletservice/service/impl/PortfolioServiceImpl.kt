package com.ampnet.walletservice.service.impl

import com.ampnet.crowdfunding.proto.TransactionsResponse
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.grpc.blockchain.pojo.PortfolioData
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.PortfolioService
import com.ampnet.walletservice.service.pojo.PortfolioStats
import com.ampnet.walletservice.service.pojo.ProjectWithInvestment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PortfolioServiceImpl(
    private val walletRepository: WalletRepository,
    private val blockchainService: BlockchainService,
    private val projectService: ProjectService
) : PortfolioService {

    @Transactional(readOnly = true)
    override fun getPortfolio(user: UUID): List<ProjectWithInvestment> {
        val userWallet = ServiceUtils.getWalletHash(user, walletRepository)
        val portfolio = blockchainService.getPortfolio(userWallet).data.associateBy { it.projectTxHash }
        return getProjectsWithInvestments(portfolio)
    }

    @Transactional(readOnly = true)
    override fun getPortfolioStats(user: UUID): PortfolioStats {
        val userWallet = ServiceUtils.getWalletHash(user, walletRepository)
        val transactions = blockchainService.getTransactions(userWallet)
        val investments = sumTransactionForType(transactions, TransactionsResponse.Transaction.Type.INVEST)
        val earnings = sumTransactionForType(transactions, TransactionsResponse.Transaction.Type.SHARE_PAYOUT)
        val dateOfFirstInvestment = transactions
            .filter { it.type == TransactionsResponse.Transaction.Type.INVEST }
            .minBy { it.date }?.date
        return PortfolioStats(investments, earnings, dateOfFirstInvestment)
    }

    @Transactional(readOnly = true)
    override fun getInvestmentsInProject(user: UUID, project: UUID): List<BlockchainTransaction> {
        val userWalletHash = ServiceUtils.getWalletHash(user, walletRepository)
        val projectWalletHash = ServiceUtils.getWalletHash(project, walletRepository)
        return blockchainService.getInvestmentsInProject(userWalletHash, projectWalletHash)
    }

    @Transactional(readOnly = true)
    override fun getTransactions(user: UUID): List<BlockchainTransaction> {
        val userWalletHash = ServiceUtils.getWalletHash(user, walletRepository)
        return blockchainService.getTransactions(userWalletHash)
    }

    private fun sumTransactionForType(
        transactions: List<BlockchainTransaction>,
        type: TransactionsResponse.Transaction.Type
    ): Long {
        return transactions
            .filter { it.type == type }
            .map { it.amount }
            .sum()
    }

    private fun getProjectsWithInvestments(portfolio: Map<String, PortfolioData>): List<ProjectWithInvestment> =
        if (portfolio.isNotEmpty()) {
            val wallets = walletRepository.findByHashes(portfolio.keys)
            val projects = projectService
                .getProjects(wallets.map { it.owner })
                .associateBy { it.uuid }

            wallets.mapNotNull { wallet ->
                portfolio[wallet.hash]?.let { portfolio ->
                    projects[wallet.owner.toString()]?.let { project ->
                        ProjectWithInvestment(project, portfolio.amount)
                    }
                }
            }
        } else {
            emptyList()
        }
}
