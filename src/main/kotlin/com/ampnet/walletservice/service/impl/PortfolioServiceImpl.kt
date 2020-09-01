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

    private val platformWalletName = "Platform"

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
        val walletHashes = getWalletHashes(blockchainTransactions)
        val wallets = walletRepository.findByHashes(walletHashes)
        return setBlockchainTransactionFromToNames(
            blockchainTransactions, wallets
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

    private fun setBlockchainTransactionFromToNames(
        blockchainTransactions: List<BlockchainTransaction>,
        wallets: List<Wallet>
    ): List<BlockchainTransaction> {
        val walletOwners = wallets.map { it.owner }.toSet()
        val walletsMap = wallets.associateBy { it.hash }
        val users = userService.getUsers(walletOwners).associateBy { it.uuid }
        val projects = projectService.getProjects(walletOwners).associateBy { it.uuid }
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
                    transaction.from = platformWalletName
                    transaction.to = getUserNameWithUuid(ownerUuidTo, users)
                }
                TransactionsResponse.Transaction.Type.WITHDRAW -> {
                    transaction.from = getUserNameWithUuid(ownerUuidFrom, users)
                    transaction.to = platformWalletName
                }
                TransactionsResponse.Transaction.Type.UNRECOGNIZED -> {
                    // skip
                }
            }
        }
        return blockchainTransactions
    }

    private fun getWalletHashes(transactions: List<BlockchainTransaction>): Set<String> {
        val walletHashes: MutableSet<String> = mutableSetOf()
        transactions.forEach { transaction ->
            walletHashes.add(transaction.fromTxHash)
            walletHashes.add(transaction.toTxHash)
        }
        return walletHashes
    }

    private fun getUserNameWithUuid(ownerUuid: UUID?, users: Map<String, UserResponse>): String? {
        return users[ownerUuid?.toString()]?.let { user ->
            "${user.firstName} ${user.lastName}"
        }
    }

    private fun getProjectNameWithUuid(ownerUuid: UUID?, projects: Map<String, ProjectResponse>): String? {
        return projects[ownerUuid?.toString()]?.name
    }
}
