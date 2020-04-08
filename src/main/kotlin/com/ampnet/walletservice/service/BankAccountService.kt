package com.ampnet.walletservice.service

import com.ampnet.walletservice.controller.pojo.request.BankAccountCreateRequest
import com.ampnet.walletservice.persistence.model.BankAccount
import java.util.UUID

interface BankAccountService {
    fun getAllBankAccounts(): List<BankAccount>
    fun createBankAccount(user: UUID, request: BankAccountCreateRequest): BankAccount
    fun deleteBankAccount(id: Int)
}
