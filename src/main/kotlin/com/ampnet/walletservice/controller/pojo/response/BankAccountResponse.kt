package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.persistence.model.BankAccount

data class BankAccountResponse(
    val id: Int,
    val iban: String,
    val bankCode: String,
    val alias: String?,
    val coop: String,
    val bankName: String?,
    val bankAddress: String?,
    val beneficiaryName: String?,
    val beneficiaryAddress: String?,
    val beneficiaryCity: String?,
    val beneficiaryCountry: String?,
) {
    constructor(bankAccount: BankAccount) : this(
        bankAccount.id, bankAccount.iban, bankAccount.bankCode, bankAccount.alias, bankAccount.coop,
        bankAccount.bankName, bankAccount.bankAddress, bankAccount.beneficiaryName, bankAccount.beneficiaryAddress,
        bankAccount.beneficiaryCity, bankAccount.beneficiaryCountry
    )
}

data class BankAccountsResponse(val bankAccounts: List<BankAccountResponse>)
