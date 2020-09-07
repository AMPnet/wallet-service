package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.persistence.model.RevenuePayout
import org.springframework.data.domain.Page
import java.time.ZonedDateTime
import java.util.UUID

data class RevenuePayoutResponse(
    val projectUuid: UUID,
    val amount: Long,
    val createdAt: ZonedDateTime,
    val createdBy: UUID,
    val txHash: String?,
    val completedAt: ZonedDateTime?
) {
    constructor(revenuePayout: RevenuePayout) : this(
        revenuePayout.projectUuid,
        revenuePayout.amount,
        revenuePayout.createdAt,
        revenuePayout.createdBy,
        revenuePayout.txHash,
        revenuePayout.completedAt
    )
}

data class RevenuePayoutsResponse(
    val revenuePayouts: List<RevenuePayoutResponse>,
    val page: Int,
    val totalPages: Int
) {
    constructor(page: Page<RevenuePayout>) : this(
        page.toList().map { RevenuePayoutResponse(it) },
        page.number,
        page.totalPages
    )
}
