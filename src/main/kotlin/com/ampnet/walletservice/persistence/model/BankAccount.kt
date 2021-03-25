package com.ampnet.walletservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "bank_account")
@Suppress("LongParameterList")
class BankAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false, length = 64)
    val iban: String,

    @Column(nullable = false, length = 16)
    val bankCode: String,

    @Column(nullable = false)
    val createdBy: UUID,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(length = 128)
    val alias: String?,

    @Column(nullable = false)
    var coop: String,

    @Column(nullable = true, length = 128)
    val bankName: String?,

    @Column(nullable = true, length = 128)
    val bankAddress: String?,

    @Column(nullable = true, length = 128)
    val beneficiaryName: String?
) {
    constructor(
        iban: String,
        bankCode: String,
        createdBy: UUID,
        alias: String?,
        coop: String,
        bankName: String?,
        bankAddress: String?,
        beneficiaryName: String?
    ) : this(
        0, iban, bankCode, createdBy, ZonedDateTime.now(),
        alias, coop, bankName, bankAddress, beneficiaryName
    )
}
