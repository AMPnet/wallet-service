package com.ampnet.walletservice.service.pojo.request

import java.util.UUID

data class RevenuePayoutTxInfoRequest(
    val projectName: String,
    val amount: Long,
    val userUuid: UUID,
    val revenuePayoutId: Int
)
