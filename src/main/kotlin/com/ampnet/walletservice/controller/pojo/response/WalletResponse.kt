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
    val hash: String?,
    val activatedAt: ZonedDateTime?,
    val alias: String?
) {
    constructor(wallet: Wallet) : this(
        wallet.uuid,
        wallet.activationData,
        wallet.type,
        wallet.currency,
        wallet.createdAt,
        wallet.hash,
        wallet.activatedAt,
        wallet.alias
    )
}
