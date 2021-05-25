package com.ampnet.walletservice.grpc.blockchain

import com.ampnet.crowdfunding.proto.ActiveSellOffersRequest
import com.ampnet.crowdfunding.proto.BalanceRequest
import com.ampnet.crowdfunding.proto.BlockchainServiceGrpc
import com.ampnet.crowdfunding.proto.CreateCooperativeRequest
import com.ampnet.crowdfunding.proto.GenerateAddWalletTxRequest
import com.ampnet.crowdfunding.proto.GenerateApproveProjectWithdrawTxRequest
import com.ampnet.crowdfunding.proto.GenerateApproveWithdrawTxRequest
import com.ampnet.crowdfunding.proto.GenerateBurnFromTxRequest
import com.ampnet.crowdfunding.proto.GenerateCancelInvestmentTxRequest
import com.ampnet.crowdfunding.proto.GenerateCreateOrganizationTxRequest
import com.ampnet.crowdfunding.proto.GenerateCreateProjectTxRequest
import com.ampnet.crowdfunding.proto.GenerateInvestTxRequest
import com.ampnet.crowdfunding.proto.GenerateMintTxRequest
import com.ampnet.crowdfunding.proto.GenerateStartRevenueSharesPayoutTxRequest
import com.ampnet.crowdfunding.proto.GenerateTransferPlatformManagerOwnershipTxRequest
import com.ampnet.crowdfunding.proto.GenerateTransferTokenIssuerOwnershipTxRequest
import com.ampnet.crowdfunding.proto.InvestmentsInProjectRequest
import com.ampnet.crowdfunding.proto.PlatformManagerRequest
import com.ampnet.crowdfunding.proto.PortfolioRequest
import com.ampnet.crowdfunding.proto.PostTxRequest
import com.ampnet.crowdfunding.proto.TokenIssuerRequest
import com.ampnet.crowdfunding.proto.TransactionType
import com.ampnet.crowdfunding.proto.TransactionsRequest
import com.ampnet.crowdfunding.proto.UserWalletsForCoopAndTxTypeRequest
import com.ampnet.walletservice.config.ApplicationProperties
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.GrpcHandledException
import com.ampnet.walletservice.grpc.blockchain.pojo.ApproveProjectBurnTransactionRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.grpc.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.Portfolio
import com.ampnet.walletservice.grpc.blockchain.pojo.PortfolioData
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.RevenuePayoutTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.SellOfferData
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import com.ampnet.crowdfunding.proto.UserWalletsForCoopAndTxTypeResponse.WalletWithHash as WalletWithHash

@Service
class BlockchainServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : BlockchainService {

    companion object : KLogging()

    private val serviceBlockingStub: BlockchainServiceGrpc.BlockchainServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("blockchain-service")
        BlockchainServiceGrpc.newBlockingStub(channel)
    }

    @Throws(GrpcException::class)
    override fun getBalance(hash: String): Long? {
        logger.debug { "Fetching balance for hash: $hash" }
        return try {
            val response = serviceWithTimeout()
                .getBalance(
                    BalanceRequest.newBuilder()
                        .setWalletTxHash(hash)
                        .build()
                )
            logger.info { "Received response: $response for hash: $hash" }
            response.balance.toLongOrNull()
        } catch (ex: StatusRuntimeException) {
            logger.warn("Could not get balance for wallet: $hash", ex)
            val grpcException =
                generateInternalExceptionFromStatusException(ex, "Could not get balance for hash: $hash")
            when (grpcException) {
                is GrpcHandledException -> return null
                else -> throw grpcException
            }
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun addWallet(activationData: String, coop: String): TransactionData {
        logger.info { "Adding wallet with activation data: $activationData for coop: $coop" }
        try {
            val response = serviceWithTimeout()
                .generateAddWalletTx(
                    GenerateAddWalletTxRequest.newBuilder()
                        .setWallet(activationData)
                        .setCoop(coop)
                        .build()
                )
            logger.info { "Successfully added wallet: $response" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex,
                "Could not add wallet: $activationData for coop: $coop"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateCreateOrganizationTransaction(userWalletHash: String): TransactionData {
        logger.info { "Generating create organization wallet: $userWalletHash" }
        try {
            val response = serviceWithTimeout()
                .generateCreateOrganizationTx(
                    GenerateCreateOrganizationTxRequest.newBuilder()
                        .setFromTxHash(userWalletHash)
                        .build()
                )
            logger.info { "Successfully created organization wallet" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex,
                "Could not generate transaction create organization: $userWalletHash"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData {
        logger.info { "Generating create project wallet transaction" }
        try {
            val response = serviceWithTimeout()
                .generateCreateProjectTx(
                    GenerateCreateProjectTxRequest.newBuilder()
                        .setFromTxHash(request.userWalletHash)
                        .setOrganizationTxHash(request.organizationHash)
                        .setMaxInvestmentPerUser(request.maxPerUser.toString())
                        .setMinInvestmentPerUser(request.minPerUser.toString())
                        .setInvestmentCap(request.investmentCap.toString())
                        .setEndInvestmentTime(request.endDateInMillis.toString())
                        .build()
                )
            logger.info { "Successfully created project wallet" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex,
                "Could not generate create Project transaction: $request"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun postTransaction(transaction: String, coop: String) =
        postTransactionWithRetries(transaction, coop, 0)

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateProjectInvestmentTransaction(request: ProjectInvestmentTxRequest): TransactionData {
        logger.info {
            "User: ${request.userWalletHash} is investing to project: ${request.projectWalletHash} " +
                "with amount ${request.amount}"
        }
        try {
            val response = serviceWithTimeout()
                .generateInvestTx(
                    GenerateInvestTxRequest.newBuilder()
                        .setFromTxHash(request.userWalletHash)
                        .setProjectTxHash(request.projectWalletHash)
                        .setAmount(request.amount.toString())
                        .build()
                )
            logger.info { "Successfully generated investment transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex, "Could not invest in project: ${request.projectWalletHash}"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateCancelInvestmentsInProject(
        userWalletHash: String,
        projectWalletHash: String
    ): TransactionData {
        logger.info { "User: $userWalletHash is canceling investments in project: $projectWalletHash" }
        try {
            val response = serviceWithTimeout()
                .generateCancelInvestmentTx(
                    GenerateCancelInvestmentTxRequest.newBuilder()
                        .setFromTxHash(userWalletHash)
                        .setProjectTxHash(projectWalletHash)
                        .build()
                )
            logger.info { "Successfully generated cancel investments in project: $projectWalletHash transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex, "Could not cancel invests in project: $projectWalletHash by user: $userWalletHash"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateMintTransaction(toHash: String, amount: Long): TransactionData {
        logger.info { "Generating Mint transaction toHash: $toHash with amount = $amount" }
        try {
            val response = serviceWithTimeout()
                .generateMintTx(
                    GenerateMintTxRequest.newBuilder()
                        .setToTxHash(toHash)
                        .setAmount(amount.toString())
                        .build()
                )
            logger.info { "Successfully generated mint transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not Mint toHash: $toHash")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateBurnTransaction(burnFromTxHash: String): TransactionData {
        logger.info { "Generating Burn transaction burnFromTxHash: $burnFromTxHash" }
        try {
            val response = serviceWithTimeout()
                .generateBurnFromTx(
                    GenerateBurnFromTxRequest.newBuilder()
                        .setBurnFromTxHash(burnFromTxHash)
                        .build()
                )
            logger.info { "Successfully generated burn transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateApproveBurnTransaction(burnFromTxHash: String, amount: Long): TransactionData {
        logger.info { "Generating Approve Burn Transaction burnFromTxHash: $burnFromTxHash with amount = $amount" }
        try {
            val response = serviceWithTimeout()
                .generateApproveWithdrawTx(
                    GenerateApproveWithdrawTxRequest.newBuilder()
                        .setFromTxHash(burnFromTxHash)
                        .setAmount(amount.toString())
                        .build()
                )
            logger.info { "Successfully generated approve burn transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateApproveProjectBurnTransaction(request: ApproveProjectBurnTransactionRequest): TransactionData {
        logger.info {
            "Generating Approve Burn Project Transaction projectTxHash: ${request.projectTxHash} " +
                "for amount = ${request.amount} by user walletHash: ${request.userWalletHash}"
        }
        try {
            val response = serviceWithTimeout()
                .generateApproveProjectWithdrawTx(
                    GenerateApproveProjectWithdrawTxRequest.newBuilder()
                        .setProjectTxHash(request.projectTxHash)
                        .setFromTxHash(request.userWalletHash)
                        .setAmount(request.amount.toString())
                        .build()
                )
            logger.info { "Successfully generated approve burn transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex, "Could not Burn Transaction for project: ${request.projectTxHash}"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateRevenuePayout(request: RevenuePayoutTxRequest): TransactionData {
        logger.info {
            "Generating Revenue Payout Transaction projectTxHash: ${request.projectWallet} " +
                "for amount = ${request.amount} by user walletHash: ${request.userWallet}"
        }
        try {
            val response = serviceWithTimeout()
                .generateStartRevenueSharesPayoutTx(
                    GenerateStartRevenueSharesPayoutTxRequest.newBuilder()
                        .setProjectTxHash(request.projectWallet)
                        .setFromTxHash(request.userWallet)
                        .setRevenue(request.amount.toString())
                        .build()
                )
            logger.info { "Successfully generated Revenue Payout Transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex, "Could not Revenue Payout Transaction for project: ${request.projectWallet}"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun getPortfolio(hash: String): Portfolio {
        logger.debug { "Get user portfolio for wallet hash: $hash" }
        try {
            val response = serviceWithTimeout()
                .getPortfolio(
                    PortfolioRequest.newBuilder()
                        .setTxHash(hash)
                        .build()
                )
            logger.debug { "Received user portfolio response, size = ${response.portfolioCount}" }
            val portfolioData = response.portfolioList.map { PortfolioData(it) }
            return Portfolio(portfolioData)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not get portfolio for wallet hash: $hash")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun getTransactions(walletHash: String): List<BlockchainTransaction> {
        logger.debug { "Get transactions for wallet hash: $walletHash" }
        try {
            val response = serviceWithTimeout()
                .getTransactions(
                    TransactionsRequest.newBuilder()
                        .setWalletHash(walletHash)
                        .build()
                )
            logger.debug { "Transactions response received, size = ${response.transactionsCount}" }
            return response.transactionsList.map { BlockchainTransaction(it) }
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex,
                "Could not get transactions for wallet hash: $walletHash"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun getInvestmentsInProject(
        userWalletAddress: String,
        projectWalletHash: String
    ): List<BlockchainTransaction> {
        logger.debug { "Get investments by user address: $userWalletAddress in project hash: $projectWalletHash" }
        try {
            val response = serviceWithTimeout()
                .getInvestmentsInProject(
                    InvestmentsInProjectRequest.newBuilder()
                        .setFromAddress(userWalletAddress)
                        .setProjectTxHash(projectWalletHash)
                        .build()
                )
            logger.debug { "Investments in project response, size = ${response.transactionsCount}" }
            return response.transactionsList.map { BlockchainTransaction(it) }
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex,
                "Could not get investments by user address: $userWalletAddress in project hash: $projectWalletHash"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun getTokenIssuer(coop: String): String {
        logger.debug { "Get token issuer for coop: $coop" }
        try {
            val response = serviceWithTimeout()
                .getTokenIssuer(
                    TokenIssuerRequest.newBuilder()
                        .setCoop(coop)
                        .build()
                )
            logger.debug { "Token issuer address: ${response.wallet}" }
            return response.wallet
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not get token issuer")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateTransferTokenIssuer(address: String, coop: String): TransactionData {
        logger.info { "Generating transfer token issuer for address: $address for coop: $coop" }
        try {
            val request = GenerateTransferTokenIssuerOwnershipTxRequest.newBuilder()
                .setNewOwnerWallet(address)
                .setCoop(coop)
                .build()
            val response = serviceWithTimeout().generateTransferTokenIssuerOwnershipTx(request)
            logger.info { "Successfully generated transfer token issuer for address: $address" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex,
                "Could not generate transfer token issuer for coop: $coop"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun getPlatformManager(coop: String): String {
        logger.debug { "Get platform manager for coop: $coop" }
        try {
            val response = serviceWithTimeout()
                .getPlatformManager(
                    PlatformManagerRequest.newBuilder()
                        .setCoop(coop)
                        .build()
                )
            logger.debug { "Platform address: ${response.wallet}" }
            return response.wallet
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not get platform manager")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun generateTransferPlatformManager(address: String, coop: String): TransactionData {
        logger.info { "Generating transfer platform manager for address: $address for coop: $coop" }
        try {
            val request = GenerateTransferPlatformManagerOwnershipTxRequest.newBuilder()
                .setNewOwnerWallet(address)
                .setCoop(coop)
                .build()
            val response = serviceWithTimeout().generateTransferPlatformManagerOwnershipTx(request)
            logger.info { "Successfully generated transfer platform manager for address: $address" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex,
                "Could not generate transfer platform manager for coop: $coop"
            )
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun getSellOffers(coop: String): List<SellOfferData> {
        logger.debug { "Get active sell offers" }
        try {
            val response = serviceWithTimeout()
                .getActiveSellOffers(
                    ActiveSellOffersRequest.newBuilder()
                        .setCoop(coop)
                        .build()
                )
            logger.debug { "Active sell offers, size = ${response.offersCount}" }
            return response.offersList.map { SellOfferData(it) }
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not get active sell offers")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun deployCoopContract(coop: String, address: String) {
        logger.info { "Deploy contract for coop: $coop with admin address: $address" }
        try {
            val request = CreateCooperativeRequest.newBuilder()
                .setCoop(coop)
                .setWallet(address)
                .build()
            serviceBlockingStub.createCooperative(request)
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(ex, "Could not get active sell offers")
        }
    }

    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun getUserWalletsWithInvestment(coop: String): List<WalletWithHash> {
        logger.debug { "Get user wallets with investment for coop: $coop" }
        try {
            val response = serviceWithTimeout()
                .getUserWalletsForCoopAndTxType(
                    UserWalletsForCoopAndTxTypeRequest.newBuilder()
                        .setCoop(coop)
                        .setType(TransactionType.INVEST)
                        .build()
                )
            logger.debug { "TransactionInfoResponse response: $response" }
            return response.walletsList
        } catch (ex: StatusRuntimeException) {
            throw generateInternalExceptionFromStatusException(
                ex, "Could not get user wallets with investment for coop: $coop"
            )
        }
    }

    private fun serviceWithTimeout() = serviceBlockingStub
        .withDeadlineAfter(applicationProperties.grpc.blockchainServiceTimeout, TimeUnit.MILLISECONDS)

    private fun postTransactionWithRetries(transaction: String, coop: String, retryCount: Int): String {
        logger.info { "Posting transaction (#$retryCount)" }
        if (retryCount > applicationProperties.grpc.blockchainServiceMaxRetries) {
            logger.warn { "Retry posting transaction exceeded" }
            throw GrpcException(ErrorCode.INT_GRPC_BLOCKCHAIN, "Retry exceeded")
        }
        try {
            val response = serviceWithTimeout()
                .postTransaction(
                    PostTxRequest.newBuilder()
                        .setData(transaction)
                        .setCoop(coop)
                        .build()
                )
            logger.info { "Successfully posted transaction: ${response.txHash}" }
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            val handledException = generateInternalExceptionFromStatusException(ex, "Couldn't post transaction")
            if (handledException is GrpcHandledException) throw handledException

            // retry posting transaction for unknown errors
            logger.warn("Failed to post transaction, retrying.", ex)
            sleep(applicationProperties.grpc.blockchainServiceRetryDelay)
            return postTransactionWithRetries(transaction, coop, retryCount + 1)
        }
    }

    private fun generateInternalExceptionFromStatusException(
        ex: StatusRuntimeException,
        message: String
    ): GrpcException {
        val grpcErrorCode = getErrorDescriptionFromExceptionStatus(ex)
            ?: return GrpcException(ErrorCode.INT_GRPC_BLOCKCHAIN, ex.localizedMessage)
        val errorCode = ErrorCode.MIDDLEWARE
        errorCode.specificCode = grpcErrorCode.code
        errorCode.message = grpcErrorCode.message
        return GrpcHandledException(errorCode, message)
    }

    // Status defined in ampnet-blockchain service, for more info see:
    // ampnet-blockchain-service/src/main/kotlin/com/ampnet/crowdfunding/blockchain/enums/ErrorCode.kt
    private fun getErrorDescriptionFromExceptionStatus(ex: StatusRuntimeException): GrpcErrorCode? {
        val description = ex.status.description?.split(" > ") ?: return null
        if (description.size != 2) return null
        return GrpcErrorCode(description[0], description[1])
    }

    private data class GrpcErrorCode(val code: String, val message: String)
}
