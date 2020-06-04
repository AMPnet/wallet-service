package com.ampnet.walletservice.grpc.blockchain.pojo

data class SellOfferData(
    val ownerWalletHash: String,
    val projectWalletHash: String,
    val shares: Long,
    val price: Long
)
