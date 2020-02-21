package com.ampnet.walletservice.grpc.blockchain.pojo

data class ApproveProjectBurnTransactionRequest(
    val projectTxHash: String,
    val amount: Long,
    val userWalletHash: String
)
