package com.ampnet.walletservice.controller.pojo.request

import javax.validation.constraints.Size

data class BankAccountCreateRequest(
    @field:Size(max = 64)
    val iban: String,
    @field:Size(max = 16)
    val bankCode: String,
    @field:Size(max = 128)
    val alias: String?,
    @field:Size(max = 128)
    val bankName: String?,
    @field:Size(max = 128)
    val bankAddress: String?,
    @field:Size(max = 128)
    val beneficiaryName: String?
)
