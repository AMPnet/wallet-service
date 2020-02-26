package com.ampnet.walletservice.service.pojo

import java.util.UUID

data class RevenuePayoutTxInfo(
    val projectName: String,
    val amount: Long,
    val userUuid: UUID,
    val revenuePayoutId: Int
)
