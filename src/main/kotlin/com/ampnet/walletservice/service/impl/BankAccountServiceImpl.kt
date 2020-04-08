package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.controller.pojo.request.BankAccountCreateRequest
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.persistence.model.BankAccount
import com.ampnet.walletservice.persistence.repository.BankAccountRepository
import com.ampnet.walletservice.service.BankAccountService
import java.util.UUID
import mu.KLogging
import org.iban4j.BicFormatException
import org.iban4j.BicUtil
import org.iban4j.Iban4jException
import org.iban4j.IbanUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BankAccountServiceImpl(private val bankAccountRepository: BankAccountRepository) : BankAccountService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getAllBankAccounts(): List<BankAccount> = bankAccountRepository.findAll()

    @Transactional
    override fun createBankAccount(user: UUID, request: BankAccountCreateRequest): BankAccount {
        validateBankCode(request.bankCode)
        validateIban(request.iban)
        logger.info { "Creating new bank account: $request" }
        val bankAccount = BankAccount(request.iban, request.bankCode, user, request.alias)
        return bankAccountRepository.save(bankAccount)
    }

    @Transactional
    override fun deleteBankAccount(id: Int) {
        logger.info { "Deleting bank account: $id" }
        bankAccountRepository.deleteById(id)
    }

    private fun validateBankCode(bankCode: String) {
        try {
            BicUtil.validate(bankCode)
        } catch (ex: BicFormatException) {
            logger.info { "Invalid bank code: $bankCode. ${ex.message}" }
            throw InvalidRequestException(ErrorCode.USER_BANK_INVALID, ex.message.orEmpty())
        }
    }

    private fun validateIban(iban: String) {
        try {
            IbanUtil.validate(iban)
        } catch (ex: Iban4jException) {
            logger.info { "Invalid IBAN: $iban. ${ex.message}" }
            throw InvalidRequestException(ErrorCode.USER_BANK_INVALID, ex.message.orEmpty())
        }
    }
}
