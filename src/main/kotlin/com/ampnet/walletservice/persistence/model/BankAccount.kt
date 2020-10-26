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
    var coop: String
) {
    constructor(iban: String, bankCode: String, createdBy: UUID, alias: String?, coop: String) : this(
        0, iban, bankCode, createdBy, ZonedDateTime.now(), alias, coop
    )
}
