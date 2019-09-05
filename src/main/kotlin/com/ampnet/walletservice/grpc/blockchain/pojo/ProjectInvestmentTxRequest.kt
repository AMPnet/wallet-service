package com.ampnet.walletservice.grpc.blockchain.pojo

data class ProjectInvestmentTxRequest(
    val userWalletHash: String,
    val projectWalletHash: String,
    val amount: Long
)
