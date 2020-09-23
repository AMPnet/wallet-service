package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.persistence.model.BankAccount

data class BankAccountResponse(
    val id: Int,
    val iban: String,
    val bankCode: String,
    val alias: String?,
    val coop: String
) {
    constructor(bankAccount: BankAccount) : this(
        bankAccount.id, bankAccount.iban,
        bankAccount.bankCode, bankAccount.alias,
        bankAccount.coop
    )
}

data class BankAccountsResponse(val bankAccounts: List<BankAccountResponse>)
