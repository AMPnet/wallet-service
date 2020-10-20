package com.ampnet.walletservice.service.pojo.request

import com.ampnet.walletservice.enums.DepositWithdrawType
import java.util.UUID

data class WithdrawCreateServiceRequest(
    val owner: UUID,
    val bankAccount: String,
    val amount: Long,
    val createBy: UUID,
    val type: DepositWithdrawType
)
