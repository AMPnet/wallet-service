package com.ampnet.walletservice.persistence.model

import com.ampnet.walletservice.enums.DepositWithdrawType
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "deposit")
data class Deposit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val ownerUuid: UUID,

    @Column(nullable = false)
    val reference: String,

    @Column(nullable = false)
    var amount: Long,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(nullable = false)
    val createdBy: UUID,

    @Column(nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    val type: DepositWithdrawType,

    @Column
    var txHash: String?,

    @Column
    var approvedByUserUuid: UUID?,

    @Column
    var approvedAt: ZonedDateTime?,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    var file: File?,

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "declined_id")
    var declined: Declined?
) {
    constructor(ownerUuid: UUID, reference: String, amount: Long, createdBy: UUID, type: DepositWithdrawType) : this(
        0, ownerUuid, reference, amount, ZonedDateTime.now(), createdBy, type,
        null, null, null, null, null
    )
}
