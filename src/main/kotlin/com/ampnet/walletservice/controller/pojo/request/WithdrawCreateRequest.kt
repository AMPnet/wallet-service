package com.ampnet.walletservice.controller.pojo.request

data class WithdrawCreateRequest(
    val amount: Long,
    val bankAccount: String
)
