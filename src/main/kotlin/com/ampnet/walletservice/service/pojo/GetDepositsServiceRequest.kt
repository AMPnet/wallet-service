package com.ampnet.walletservice.service.pojo

import com.ampnet.walletservice.enums.DepositWithdrawType

data class GetDepositsServiceRequest(
    val approved: Boolean,
    val type: DepositWithdrawType,
    val coop: String
)
