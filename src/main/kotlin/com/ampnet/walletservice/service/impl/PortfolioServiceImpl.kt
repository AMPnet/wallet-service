package com.ampnet.walletservice.service.impl

import com.ampnet.crowdfunding.proto.TransactionsResponse
import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.grpc.blockchain.pojo.PortfolioData
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Wallet
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
    private val projectService: ProjectService,
    private val userService: UserService
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
        val blockchainTransactions = blockchainService.getTransactions(userWalletHash)
        val setOfWalletHashes: MutableSet<String> = mutableSetOf()
        blockchainTransactions.forEach { transaction ->
            setOfWalletHashes.add(transaction.fromTxHash)
            setOfWalletHashes.add(transaction.toTxHash)
        }
        val wallets = walletRepository.findByHashes(setOfWalletHashes)
        val walletsOwnerMap = wallets.associateBy { it.owner }
        val walletsHashMap = wallets.associateBy { it.hash }

        val users = userService.getUsers(walletsOwnerMap.keys).associateBy { it.uuid }
        val projects = projectService.getProjects(walletsOwnerMap.keys).associateBy { it.uuid }

        return mapBlockchainTrxBasedOnTransactionType(
            blockchainTransactions, users, projects, walletsHashMap
        )
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

    private fun mapBlockchainTrxBasedOnTransactionType(
        blockchainTransactions: List<BlockchainTransaction>,
        users: Map<String, UserResponse>,
        projects: Map<String, ProjectResponse>,
        walletsMap: Map<String?, Wallet>
    ): List<BlockchainTransaction> {
        blockchainTransactions.forEach { transaction ->
            val ownerUuidFrom = walletsMap[transaction.fromTxHash]?.owner
            val ownerUuidTo = walletsMap[transaction.toTxHash]?.owner
            when (transaction.type) {
                TransactionsResponse.Transaction.Type.INVEST -> {
                    transaction.from = getUserNameWithUuid(ownerUuidFrom, users)
                    transaction.to = getProjectNameWithUuid(ownerUuidTo, projects)
                }
                TransactionsResponse.Transaction.Type.CANCEL_INVESTMENT -> {
                    transaction.from = getProjectNameWithUuid(ownerUuidFrom, projects)
                    transaction.to = getUserNameWithUuid(ownerUuidTo, users)
                }
                TransactionsResponse.Transaction.Type.SHARE_PAYOUT -> {
                    transaction.from = getProjectNameWithUuid(ownerUuidFrom, projects)
                    transaction.to = getUserNameWithUuid(ownerUuidTo, users)
                }
                TransactionsResponse.Transaction.Type.DEPOSIT -> {
                    transaction.from = null
                    transaction.to = getUserNameWithUuid(ownerUuidTo, users)
                }
                TransactionsResponse.Transaction.Type.WITHDRAW -> {
                    transaction.from = getUserNameWithUuid(ownerUuidFrom, users)
                    transaction.to = null
                }
                else -> {
                    transaction.from = null
                    transaction.to = null
                }
            }
        }
        return blockchainTransactions
    }

    private fun getUserNameWithUuid(ownerUuid: UUID?, users: Map<String, UserResponse>): String? {
        val user = users[ownerUuid.toString()]
        if (ownerUuid == null || user == null) {
            return null
        }
        return "${user.firstName} ${user.lastName}"
    }

    private fun getProjectNameWithUuid(ownerUuid: UUID?, projects: Map<String, ProjectResponse>): String? {
        val project = projects[ownerUuid.toString()]
        if (ownerUuid == null || project == null) {
            return null
        }
        return project.name
    }
}
