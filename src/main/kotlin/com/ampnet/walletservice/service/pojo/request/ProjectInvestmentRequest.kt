package com.ampnet.walletservice.service.pojo.request

import java.util.UUID

data class ProjectInvestmentRequest(
    val projectUuid: UUID,
    val investorUuid: UUID,
    val amount: Long
)
