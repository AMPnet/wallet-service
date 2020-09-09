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
@Table(name = "revenue_payout")
data class RevenuePayout(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val projectUuid: UUID,

    @Column(nullable = false)
    var amount: Long,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(nullable = false)
    val createdBy: UUID,

    @Column
    var txHash: String?,

    @Column
    var completedAt: ZonedDateTime?,

    @Column(nullable = false)
    var coop: String
) {
    constructor(projectUuid: UUID, amount: Long, user: UUID, coop: String = "ampnet") : this(
        0,
        projectUuid,
        amount,
        ZonedDateTime.now(),
        user,
        null,
        null,
        coop
    )
}
