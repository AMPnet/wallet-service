package com.ampnet.walletservice.controller.pojo.request

data class BankAccountCreateRequest(
    val iban: String,
    val bankCode: String,
    val alias: String?
)
