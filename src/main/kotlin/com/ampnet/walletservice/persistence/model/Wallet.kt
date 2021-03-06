package com.ampnet.walletservice.persistence.model

import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "wallet")
@Suppress("LongParameterList")
class Wallet(
    @Id
    val uuid: UUID,

    @Column(nullable = false)
    val owner: UUID,

    @Column(nullable = false, length = 128)
    val activationData: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    val type: WalletType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    val currency: Currency,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(length = 128)
    var hash: String?,

    @Column
    var activatedAt: ZonedDateTime?,

    @Column(nullable = false)
    var coop: String,

    @Column(length = 128)
    var email: String?,

    @Column
    var providerId: String?

) {
    constructor(
        owner: UUID,
        activationData: String,
        type: WalletType,
        currency: Currency,
        coop: String,
        email: String? = null,
        providerId: String? = null
    ) : this(
        UUID.randomUUID(),
        owner,
        activationData,
        type,
        currency,
        ZonedDateTime.now(),
        null,
        null,
        coop,
        email,
        providerId
    )
}
