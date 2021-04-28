package com.ampnet.walletservice.persistence.model

import com.ampnet.walletservice.controller.pojo.request.BankAccountCreateRequest
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
    val beneficiaryName: String?,

    @Column(nullable = true, length = 256)
    val beneficiaryAddress: String?,

    @Column(nullable = true, length = 64)
    val beneficiaryCity: String?,

    @Column(nullable = true, length = 64)
    val beneficiaryCountry: String?
) {
    constructor(request: BankAccountCreateRequest, createdBy: UUID, coop: String) : this(
        0, request.iban, request.bankCode, createdBy, ZonedDateTime.now(),
        request.alias, coop, request.bankName, request.bankAddress, request.beneficiaryName,
        request.beneficiaryAddress, request.beneficiaryCity, request.beneficiaryCountry
    )
}
