package com.ampnet.walletservice.service.pojo.request

import com.ampnet.core.jwt.UserPrincipal

data class RevenuePayoutTxInfoRequest(
    val projectName: String,
    val amount: Long,
    val user: UserPrincipal,
    val revenuePayoutId: Int
)
