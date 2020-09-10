package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.BankAccountCreateRequest
import com.ampnet.walletservice.controller.pojo.response.BankAccountResponse
import com.ampnet.walletservice.controller.pojo.response.BankAccountsResponse
import com.ampnet.walletservice.enums.PrivilegeType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.persistence.model.BankAccount
import com.ampnet.walletservice.persistence.repository.BankAccountRepository
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BankAccountControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var bankAccountRepository: BankAccountRepository

    private val bankAccountPath = "/bank-account"
    private val iban = "HR1723600001101234565"
    private val bankCode = "DABAIE2D"
    private val alias = "alias"
    private lateinit var bankAccount: BankAccount

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllBankAccounts()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetAllBankAccounts() {
        suppose("There are two bank accounts") {
            val bankAccount = BankAccount(iban, bankCode, userUuid, alias, COOP)
            bankAccountRepository.save(bankAccount)
            val secondBankAccount = BankAccount("AL47212110090000000235698741", "AKIVALTR", userUuid, "albalias", COOP)
            bankAccountRepository.save(secondBankAccount)
        }

        verify("User can get bank accounts") {
            val result = mockMvc.perform(get(bankAccountPath))
                .andExpect(status().isOk)
                .andReturn()

            val bankAccounts: BankAccountsResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(bankAccounts.bankAccounts).hasSize(2)
            val bankAccount = bankAccounts.bankAccounts.first()
            assertThat(bankAccount.iban).isEqualTo(iban)
            assertThat(bankAccount.bankCode).isEqualTo(bankAccount.bankCode)
            assertThat(bankAccount.alias).isEqualTo(alias)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToCreateBankAccount() {
        verify("Admin can create bank account") {
            val request = BankAccountCreateRequest(iban, bankCode, alias)
            val result = mockMvc.perform(
                post(bankAccountPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            val bankAccount: BankAccountResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(bankAccount.iban).isEqualTo(iban)
            assertThat(bankAccount.bankCode).isEqualTo(bankAccount.bankCode)
            assertThat(bankAccount.alias).isEqualTo(alias)
        }
        verify("Bank account is created") {
            val bankAccount = bankAccountRepository.findAll().first()
            assertThat(bankAccount.iban).isEqualTo(iban)
            assertThat(bankAccount.bankCode).isEqualTo(bankAccount.bankCode)
            assertThat(bankAccount.alias).isEqualTo(alias)
            assertThat(bankAccount.createdBy).isEqualTo(userUuid)
            assertThat(bankAccount.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustThrowExceptionForTooLongBankAccountAlias() {
        verify("Admin can create bank account") {
            val request = BankAccountCreateRequest(iban, bankCode, "aaa".repeat(55))
            val result = mockMvc.perform(
                post(bankAccountPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.INT_DB)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToDeleteBankAccount() {
        suppose("There is bank account") {
            bankAccount = BankAccount(iban, bankCode, userUuid, alias, COOP)
            bankAccountRepository.save(bankAccount)
        }

        verify("Admin can delete bank account") {
            mockMvc.perform(delete("$bankAccountPath/${bankAccount.id}"))
                .andExpect(status().isOk)
        }
        verify("Bank account is deleted") {
            val bankAccounts = bankAccountRepository.findAll()
            assertThat(bankAccounts).isEmpty()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustNotBeAbleToCreateBankAccountWithInvalidBankCode() {
        verify("Admin cannot create bank account with invalid bank code") {
            val request = BankAccountCreateRequest(iban, "invalid", alias)
            val result = mockMvc.perform(
                post(bankAccountPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.USER_BANK_INVALID)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustNotBeAbleToCreateBankAccountWithInvalidIban() {
        val request = BankAccountCreateRequest("invalid-iban", bankCode, alias)
        val result = mockMvc.perform(
            post(bankAccountPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andReturn()

        verifyResponseErrorCode(result, ErrorCode.USER_BANK_INVALID)
    }
}
