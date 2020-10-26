package com.ampnet.walletservice.service.pojo.request

import com.ampnet.core.jwt.UserPrincipal
import java.util.UUID

data class ProjectInvestmentRequest(
    val projectUuid: UUID,
    val investor: UserPrincipal,
    val amount: Long
)
