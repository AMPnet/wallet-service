package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.controller.pojo.request.BankAccountCreateRequest
import com.ampnet.walletservice.persistence.model.BankAccount
import com.ampnet.walletservice.persistence.repository.BankAccountRepository
import com.ampnet.walletservice.service.BankAccountService
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class BankAccountServiceImpl(private val bankAccountRepository: BankAccountRepository) : BankAccountService {

    override fun getAllBankAccounts(): List<BankAccount> = bankAccountRepository.findAll()

    override fun createBankAccount(user: UUID, request: BankAccountCreateRequest): BankAccount {
        val bankAccount = BankAccount(request.iban, request.bankCode, user, request.alias)
        return bankAccountRepository.save(bankAccount)
    }

    override fun deleteBankAccount(id: Int) = bankAccountRepository.deleteById(id)
}
