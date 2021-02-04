package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.controller.pojo.request.BankAccountCreateRequest
import com.ampnet.walletservice.persistence.model.BankAccount

interface BankAccountService {
    fun getAllBankAccounts(coop: String): List<BankAccount>
    fun createBankAccount(user: UserPrincipal, request: BankAccountCreateRequest): BankAccount
    fun deleteBankAccount(id: Int)
    fun validateIban(iban: String)
    fun validateBankCode(bankCode: String)
}
