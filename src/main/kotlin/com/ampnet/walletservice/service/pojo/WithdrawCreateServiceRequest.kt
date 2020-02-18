package com.ampnet.walletservice.service.pojo

import com.ampnet.walletservice.enums.WalletType
import java.util.UUID

data class WithdrawCreateServiceRequest(
    val owner: UUID,
    val bankAccount: String,
    val amount: Long,
    val createBy: UUID,
    val type: WalletType
)
