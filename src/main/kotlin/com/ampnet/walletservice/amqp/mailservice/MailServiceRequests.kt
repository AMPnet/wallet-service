package com.ampnet.walletservice.amqp.mailservice

import java.util.UUID

data class DepositInfoRequest(
    val user: UUID,
    val minted: Boolean
)

data class WithdrawRequest(
    val user: UUID,
    val amount: Long
)

data class WithdrawInfoRequest(
    val user: UUID,
    val burned: Boolean
)

data class NewWalletRequest(
    val type: WalletTypeAmqp,
    val coop: String,
    val activationData: String
)

data class WalletActivatedRequest(
    val type: WalletTypeAmqp,
    val walletOwner: UUID,
    val activationData: String
)

enum class WalletTypeAmqp {
    USER, PROJECT, ORGANIZATION
}
