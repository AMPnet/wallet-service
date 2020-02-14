package com.ampnet.walletservice.grpc.blockchain

import com.ampnet.crowdfunding.proto.BalanceRequest
import com.ampnet.crowdfunding.proto.BlockchainServiceGrpc
import com.ampnet.crowdfunding.proto.GenerateAddWalletTxRequest
import com.ampnet.crowdfunding.proto.GenerateApproveWithdrawTxRequest
import com.ampnet.crowdfunding.proto.GenerateBurnFromTxRequest
import com.ampnet.crowdfunding.proto.GenerateCreateOrganizationTxRequest
import com.ampnet.crowdfunding.proto.GenerateCreateProjectTxRequest
import com.ampnet.crowdfunding.proto.GenerateInvestTxRequest
import com.ampnet.crowdfunding.proto.GenerateMintTxRequest
import com.ampnet.crowdfunding.proto.GetProjectsInfoRequest
import com.ampnet.crowdfunding.proto.InvestmentsInProjectRequest
import com.ampnet.crowdfunding.proto.PortfolioRequest
import com.ampnet.crowdfunding.proto.PostTxRequest
import com.ampnet.crowdfunding.proto.TransactionsRequest
import com.ampnet.walletservice.config.ApplicationProperties
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.grpc.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.Portfolio
import com.ampnet.walletservice.grpc.blockchain.pojo.PortfolioData
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInfoResponse
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import io.grpc.StatusRuntimeException
import java.util.concurrent.TimeUnit
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

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

    override fun getBalance(hash: String): Long {
        logger.debug { "Fetching balance for hash: $hash" }
        try {
            val response = serviceWithTimeout()
                .getBalance(
                    BalanceRequest.newBuilder()
                        .setWalletTxHash(hash)
                        .build()
                )
            logger.info { "Received response: $response" }
            return response.balance.toLongOrNull()
                ?: throw GrpcException(ErrorCode.INT_GRPC_BLOCKCHAIN, "Cannot get balance as number")
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not get balance for wallet: $hash")
        }
    }

    override fun addWallet(activationData: String): TransactionData {
        logger.info { "Adding wallet with activation data: $activationData" }
        try {
            val response = serviceWithTimeout()
                .generateAddWalletTx(
                    GenerateAddWalletTxRequest.newBuilder()
                        .setWallet(activationData)
                        .build()
                )
            logger.info { "Successfully added wallet: $response" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not add wallet: $activationData")
        }
    }

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
            throw getInternalExceptionFromStatusException(ex,
                "Could not generate transaction create organization: $userWalletHash")
        }
    }

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
            throw getInternalExceptionFromStatusException(ex, "Could not generate create Project transaction: $request")
        }
    }

    override fun postTransaction(transaction: String): String {
        logger.info { "Posting transaction" }
        try {
            val response = serviceWithTimeout()
                .postTransaction(
                    PostTxRequest.newBuilder()
                        .setData(transaction)
                        .build()
                )
            logger.info { "Successfully posted transaction: ${response.txHash}" }
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not post transaction: $transaction")
        }
    }

    override fun generateProjectInvestmentTransaction(request: ProjectInvestmentTxRequest): TransactionData {
        logger.info { "User: ${request.userWalletHash} is investing to project: ${request.projectWalletHash} " +
            "with amount ${request.amount}" }
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
            throw getInternalExceptionFromStatusException(
                ex, "Could not invest in project: ${request.projectWalletHash}")
        }
    }

    override fun generateCancelInvestmentsInProject(
        userWalletHash: String,
        projectWalletHash: String
    ): TransactionData {
        logger.info { "User: $userWalletHash is canceling investments in project: $projectWalletHash" }
        try {
            // TODO: generate cancel investments in project!
            val response = serviceWithTimeout()
                .generateInvestTx(
                    GenerateInvestTxRequest.newBuilder()
                        .setFromTxHash(userWalletHash)
                        .setProjectTxHash(projectWalletHash)
                        .build()
                )
            logger.info { "Successfully generated cancel investments in project: $projectWalletHash transaction" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(
                ex, "Could not cancel invests in project: $projectWalletHash by user: $userWalletHash")
        }
    }

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
            throw getInternalExceptionFromStatusException(ex, "Could not Mint toHash: $toHash")
        }
    }

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
            throw getInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

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
            throw getInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

    override fun getPortfolio(hash: String): Portfolio {
        logger.debug { "Get user portfolio for wallet hash: $hash" }
        try {
            val response = serviceWithTimeout()
                .getPortfolio(
                    PortfolioRequest.newBuilder()
                        .setTxHash(hash)
                        .build()
                )
            logger.debug { "User portfolio response: $response" }
            val portfolioData = response.portfolioList.map { PortfolioData(it) }
            return Portfolio(portfolioData)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not get portfolio for wallet: $hash")
        }
    }

    override fun getTransactions(hash: String): List<BlockchainTransaction> {
        logger.debug { "Get transactions for wallet hash: $hash" }
        try {
            val response = serviceWithTimeout()
                .getTransactions(
                    TransactionsRequest.newBuilder()
                        .setTxHash(hash)
                        .build()
                )
            logger.debug { "Transactions response: $response" }
            return response.transactionsList.map { BlockchainTransaction(it) }
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not get transactions for wallet: $hash")
        }
    }

    override fun getInvestmentsInProject(
        userWalletHash: String,
        projectWalletHash: String
    ): List<BlockchainTransaction> {
        logger.debug { "Get investments by user: $userWalletHash in project: $projectWalletHash" }
        try {
            val response = serviceWithTimeout()
                .getInvestmentsInProject(
                    InvestmentsInProjectRequest.newBuilder()
                        .setFromTxHash(userWalletHash)
                        .setProjectTxHash(projectWalletHash)
                        .build()
                )
            logger.debug { "Investments in project response: $response" }
            return response.transactionsList.map { BlockchainTransaction(it) }
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex,
                "Could not get investments by user: $userWalletHash in project: $projectWalletHash")
        }
    }

    override fun getProjectsInfo(hashes: List<String>): List<ProjectInfoResponse> {
        logger.debug { "Get projects info for hashes: $hashes" }
        try {
            val response = serviceWithTimeout()
                .getProjectsInfo(
                    GetProjectsInfoRequest.newBuilder()
                        .addAllProjectTxHashes(hashes)
                        .build()
                )
            logger.debug { "Projects info response: $response" }
            return response.projectsList.map { ProjectInfoResponse(it) }
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex,
                "Could not get projects info for hashes: $hashes")
        }
    }

    private fun serviceWithTimeout() = serviceBlockingStub
        .withDeadlineAfter(applicationProperties.grpc.blockchainServiceTimeout, TimeUnit.MILLISECONDS)

    private fun getInternalExceptionFromStatusException(
        ex: StatusRuntimeException,
        message: String
    ): GrpcException {
        val grpcErrorCode = getErrorDescriptionFromExceptionStatus(ex)
        val errorCode = ErrorCode.INT_GRPC_BLOCKCHAIN
        errorCode.specificCode = grpcErrorCode.code
        errorCode.message = grpcErrorCode.message
        return GrpcException(errorCode, message)
    }

    // Status defined in ampenet-blockchain service, for more info see:
    // ampnet-blockchain-service/src/main/kotlin/com/ampnet/crowdfunding/blockchain/enums/ErrorCode.kt
    private fun getErrorDescriptionFromExceptionStatus(ex: StatusRuntimeException): GrpcErrorCode {
        val description = ex.status.description?.split(" > ") ?: throw ex
        if (description.size != 2) {
            throw ex
        }
        return GrpcErrorCode(description[0], description[1])
    }

    private data class GrpcErrorCode(val code: String, val message: String)
}
