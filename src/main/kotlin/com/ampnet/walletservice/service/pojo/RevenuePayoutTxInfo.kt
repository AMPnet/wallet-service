package com.ampnet.walletservice.service.pojo

import com.ampnet.core.jwt.UserPrincipal

data class RevenuePayoutTxInfo(
    val projectName: String,
    val amount: Long,
    val user: UserPrincipal,
    val revenuePayoutId: Int
)
