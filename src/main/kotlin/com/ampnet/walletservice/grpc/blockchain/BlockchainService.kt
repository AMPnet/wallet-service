package com.ampnet.walletservice.grpc.blockchain

import com.ampnet.crowdfunding.proto.TransactionState
import com.ampnet.walletservice.grpc.blockchain.pojo.ApproveProjectBurnTransactionRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.grpc.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.Portfolio
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInfoResponse
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.RevenuePayoutTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.SellOfferData
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData

interface BlockchainService {
    fun getBalance(hash: String): Long
    fun addWallet(activationData: String): TransactionData
    fun generateCreateOrganizationTransaction(userWalletHash: String): TransactionData
    fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData
    fun postTransaction(transaction: String): String
    fun generateProjectInvestmentTransaction(request: ProjectInvestmentTxRequest): TransactionData
    fun generateCancelInvestmentsInProject(userWalletHash: String, projectWalletHash: String): TransactionData
    fun generateMintTransaction(toHash: String, amount: Long): TransactionData
    fun generateBurnTransaction(burnFromTxHash: String): TransactionData
    fun generateApproveBurnTransaction(burnFromTxHash: String, amount: Long): TransactionData
    fun generateApproveProjectBurnTransaction(request: ApproveProjectBurnTransactionRequest): TransactionData
    fun generateRevenuePayout(request: RevenuePayoutTxRequest): TransactionData
    fun getPortfolio(hash: String): Portfolio
    fun getTransactions(hash: String): List<BlockchainTransaction>
    fun getInvestmentsInProject(userWalletHash: String, projectWalletHash: String): List<BlockchainTransaction>
    fun getProjectsInfo(hashes: List<String>): List<ProjectInfoResponse>
    fun getTokenIssuer(coop: String): String
    fun generateTransferTokenIssuer(address: String): TransactionData
    fun getPlatformManager(coop: String): String
    fun generateTransferPlatformManager(address: String): TransactionData
    fun getTransactionState(txHash: String): TransactionState
    fun getSellOffers(): List<SellOfferData>
}
