package com.ampnet.walletservice.controller.pojo.request

import javax.validation.constraints.Positive

data class WithdrawCreateRequest(
    @field:Positive
    val amount: Long,
    val bankAccount: String
)
