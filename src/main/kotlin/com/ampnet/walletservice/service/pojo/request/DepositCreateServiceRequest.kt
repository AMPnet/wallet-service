package com.ampnet.walletservice.service.pojo.request

import com.ampnet.walletservice.enums.DepositWithdrawType
import java.util.UUID

data class DepositCreateServiceRequest(
    val owner: UUID,
    val createdBy: UUID,
    val amount: Long,
    val type: DepositWithdrawType
)
