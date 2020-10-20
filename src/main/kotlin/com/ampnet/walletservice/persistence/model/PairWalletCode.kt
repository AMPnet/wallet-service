package com.ampnet.walletservice.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "pair_wallet_code")
class PairWalletCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false, length = 128)
    val publicKey: String,

    @Column(nullable = false, length = 6)
    val code: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
)
