package com.ampnet.walletservice.service.impl

import com.ampnet.crowdfunding.proto.TransactionState
import com.ampnet.crowdfunding.proto.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.GrpcHandledException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.grpc.blockchain.pojo.PortfolioData
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.PortfolioService
import com.ampnet.walletservice.service.pojo.response.PortfolioStats
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import com.ampnet.walletservice.service.pojo.response.ProjectWithInvestment
import com.ampnet.walletservice.service.pojo.response.UserServiceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.util.UUID

const val SCALE = 10

@Service
@Suppress("TooManyFunctions")
class PortfolioServiceImpl(
    private val walletRepository: WalletRepository,
    private val blockchainService: BlockchainService,
    private val projectService: ProjectService,
    private val userService: UserService
) : PortfolioService {

    private val platformWalletName = "Platform"

    @Transactional(readOnly = true)
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun getPortfolio(user: UUID): List<ProjectWithInvestment> {
        val walletHash = ServiceUtils.getWalletHash(user, walletRepository)
        val portfolio = blockchainService.getPortfolio(walletHash).data
            .associateBy { it.projectTxHash }
        return getProjectsWithInvestments(portfolio)
    }

    @Transactional(readOnly = true)
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun getPortfolioStats(user: UUID): PortfolioStats {
        val walletHash = ServiceUtils.getWalletHash(user, walletRepository)
        val transactions = blockchainService.getTransactions(walletHash)
            .filter { it.state == TransactionState.MINED }
        val investments = sumTransactionForType(transactions, TransactionType.INVEST)
        val cancelInvestments = sumTransactionForType(transactions, TransactionType.CANCEL_INVESTMENT)
        val earnings = sumTransactionForType(transactions, TransactionType.SHARE_PAYOUT)
        val dateOfFirstInvestment = transactions
            .filter { it.type == TransactionType.INVEST }
            .minByOrNull { it.date }?.date
        return PortfolioStats(investments - cancelInvestments, earnings, dateOfFirstInvestment)
    }

    @Transactional(readOnly = true)
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun getInvestmentsInProject(user: UUID, project: UUID): List<BlockchainTransaction> {
        val userWalletAddress = ServiceUtils.getWalletByUserUuid(user, walletRepository).activationData
        val projectWalletHash = ServiceUtils.getWalletHash(project, walletRepository)
        return blockchainService.getInvestmentsInProject(userWalletAddress, projectWalletHash)
    }

    @Transactional(readOnly = true)
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun getTransactions(user: UUID): List<BlockchainTransaction> {
        val walletHash = ServiceUtils.getWalletHash(user, walletRepository)
        val blockchainTransactions = blockchainService.getTransactions(walletHash)
        val walletHashes = getWalletHashes(blockchainTransactions)
        val wallets = walletRepository.findByHashes(walletHashes)
        return setBlockchainTransactionFromToNames(blockchainTransactions, wallets)
    }

    @Transactional(readOnly = true)
    @Throws(
        ResourceNotFoundException::class,
        InvalidRequestException::class,
        GrpcException::class,
        GrpcHandledException::class
    )
    override fun getProjectTransactions(projectUuid: UUID, userUuid: UUID): List<BlockchainTransaction> {
        throwExceptionIfUserNotMemberOfOrganization(projectUuid, userUuid)
        val walletHash = ServiceUtils.getWalletHash(projectUuid, walletRepository)
        val transactions = blockchainService.getTransactions(walletHash)
        val walletHashes = getWalletHashes(transactions)
        val wallets = walletRepository.findByHashes(walletHashes)
        return setBlockchainTransactionFromToNames(transactions, wallets)
    }

    private fun sumTransactionForType(transactions: List<BlockchainTransaction>, type: TransactionType): Long {
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
                    projects[wallet.owner]?.let { project ->
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
                TransactionType.INVEST,
                TransactionType.APPROVE_INVESTMENT -> {
                    transaction.from = getUserNameWithUuid(ownerUuidFrom, users)
                    transaction.to = getProjectNameWithUuid(ownerUuidTo, projects)
                    transaction.description = transaction.to
                    transaction.share = getExpectedProjectFunding(ownerUuidTo, projects)?.let {
                        getShare(it, transaction.amount)
                    }
                }
                TransactionType.CANCEL_INVESTMENT -> {
                    transaction.from = getProjectNameWithUuid(ownerUuidFrom, projects)
                    transaction.to = getUserNameWithUuid(ownerUuidTo, users)
                    transaction.description = transaction.from
                    transaction.share = getExpectedProjectFunding(ownerUuidFrom, projects)?.let {
                        getShare(it, transaction.amount)
                    }
                }
                TransactionType.SHARE_PAYOUT -> {
                    transaction.from = getProjectNameWithUuid(ownerUuidFrom, projects)
                    transaction.to = getUserNameWithUuid(ownerUuidTo, users)
                    transaction.description = transaction.from
                }
                TransactionType.DEPOSIT -> {
                    transaction.from = platformWalletName
                    transaction.to = getUserNameWithUuid(ownerUuidTo, users)
                }
                TransactionType.WITHDRAW -> {
                    transaction.from = getUserNameWithUuid(ownerUuidFrom, users)
                    transaction.to = platformWalletName
                }
                else -> {
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

    private fun getUserNameWithUuid(ownerUuid: UUID?, users: Map<UUID, UserServiceResponse>): String? =
        users[ownerUuid]?.let { user ->
            "${user.firstName} ${user.lastName}"
        }

    private fun getProjectNameWithUuid(ownerUuid: UUID?, projects: Map<UUID, ProjectServiceResponse>): String? =
        projects[ownerUuid]?.name

    private fun getExpectedProjectFunding(ownerUuid: UUID?, projects: Map<UUID, ProjectServiceResponse>): Long? =
        projects[ownerUuid]?.expectedFunding

    private fun getShare(projectFunding: Long, amount: Long): String {
        return amount.toBigDecimal().divide(
            projectFunding.toBigDecimal(), SCALE, RoundingMode.HALF_UP
        ).toPlainString()
    }

    private fun throwExceptionIfUserNotMemberOfOrganization(projectUuid: UUID, userUuid: UUID) {
        val memberInOrganization =
            projectService.getOrganizationMembersForProject(projectUuid).any { it.userUuid == userUuid.toString() }
        if (memberInOrganization.not()) {
            throw InvalidRequestException(
                ErrorCode.ORG_MEM_MISSING,
                "User: $userUuid is not a member of project: $projectUuid"
            )
        }
    }
}
