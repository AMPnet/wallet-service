package com.ampnet.walletservice.grpc.blockchain.pojo

data class GenerateProjectWalletRequest(
    val userWalletHash: String,
    val organizationHash: String,
    val maxPerUser: Long,
    val minPerUser: Long,
    val investmentCap: Long,
    val endDateInMillis: Long
)
