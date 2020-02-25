package com.ampnet.walletservice.grpc.blockchain.pojo

data class RevenuePayoutTxRequest(
    val userWallet: String,
    val projectWallet: String,
    val amount: Long
)
