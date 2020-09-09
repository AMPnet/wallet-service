package com.ampnet.walletservice.service.pojo

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.DepositWithdrawType
import java.util.UUID

data class DepositCreateServiceRequest(
    val owner: UUID,
    val createdBy: UserPrincipal,
    val amount: Long,
    val type: DepositWithdrawType
)
