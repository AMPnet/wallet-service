package com.ampnet.walletservice.controller.pojo.request

import javax.validation.constraints.Positive

data class AmountRequest(
    @get:Positive
    val amount: Long
)
