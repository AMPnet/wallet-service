package com.ampnet.walletservice.amqp.blockchainservice

data class ActivateWalletMessage(
    val address: String,
    val coop: String,
    val hash: String
)

data class UpdateCoopRolesMessage(
    val coop: String
)
