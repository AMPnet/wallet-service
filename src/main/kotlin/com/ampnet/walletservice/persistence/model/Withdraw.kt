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
@Table(name = "withdraw")
@Suppress("LongParameterList")
class Withdraw(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val ownerUuid: UUID,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(nullable = false)
    val createdBy: UUID,

    @Column(nullable = false, length = 64)
    val bankAccount: String,

    @Column
    var approvedTxHash: String?,

    @Column
    var approvedAt: ZonedDateTime?,

    @Column
    var burnedTxHash: String?,

    @Column
    var burnedAt: ZonedDateTime?,

    @Column
    var burnedBy: UUID?,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    var file: File?,

    @Column(nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    val type: DepositWithdrawType
)
