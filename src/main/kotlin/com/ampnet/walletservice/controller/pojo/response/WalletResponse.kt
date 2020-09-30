package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import java.time.ZonedDateTime
import java.util.UUID

data class WalletResponse(
    val uuid: UUID,
    val activationData: String,
    val type: WalletType,
    val currency: Currency,
    val createdAt: ZonedDateTime,
    val coop: String,
    val hash: String?,
    val activatedAt: ZonedDateTime?,
    val email: String?,
    val balance: Long?,
    val providerId: String?
) {
    constructor(wallet: Wallet, balance: Long? = null) : this(
        wallet.uuid,
        wallet.activationData,
        wallet.type,
        wallet.currency,
        wallet.createdAt,
        wallet.coop,
        wallet.hash,
        wallet.activatedAt,
        wallet.email,
        balance,
        wallet.providerId
    )
}
