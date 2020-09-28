package com.ampnet.walletservice.controller.pojo.request

data class WalletCreateRequest(
    val publicKey: String,
    val email: String?,
    val providerId: String?
)
