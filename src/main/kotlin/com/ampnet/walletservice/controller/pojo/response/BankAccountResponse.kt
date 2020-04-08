package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.persistence.model.BankAccount

data class BankAccountResponse(
    val id: Int,
    val iban: String,
    val bankCode: String,
    val alias: String?
) {
    constructor(bankAccount: BankAccount) : this(
        bankAccount.id, bankAccount.iban, bankAccount.bankCode, bankAccount.alias
    )
}
data class BankAccountsResponse(val bankAccounts: List<BankAccountResponse>)
