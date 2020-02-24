package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class DepositControllerTest : ControllerTestBase() {

    private val depositPath = "/deposit"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateDeposit() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User has approved deposit") {
            createApprovedDeposit(userUuid, "tx_hash")
        }

        verify("User can create new deposit") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                    post(depositPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.user).isEqualTo(userUuid)
            assertThat(deposit.amount).isEqualTo(testContext.amount)
            assertThat(deposit.approved).isFalse()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("User deposit is stored") {
            val deposits = depositRepository.findAll()
            assertThat(deposits).hasSize(2)
            val deposit = deposits.first { it.approved.not() }
            assertThat(deposit.userUuid).isEqualTo(userUuid)
            assertThat(deposit.approved).isFalse()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Mail notification is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendDepositRequest(userUuid, testContext.amount)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateDepositWithoutWallet() {
        suppose("User does not has a wallet") {
            databaseCleanerService.deleteAllWallets()
        }

        verify("User cannot create deposit without wallet") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                    post(depositPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetPendingDeposit() {
        suppose("User deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("User can his get pending deposit") {
            val result = mockMvc.perform(get(depositPath))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.user).isEqualTo(userUuid)
            val savedDeposit = testContext.deposits.first()
            assertThat(deposit.id).isEqualTo(savedDeposit.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetNotFoundForNoPendingDeposit() {
        verify("User gets not found for non pending deposit") {
            mockMvc.perform(get(depositPath))
                    .andExpect(MockMvcResultMatchers.status().isNotFound)
        }
    }

    private class TestContext {
        val amount = 30_000L
        var deposits = listOf<Deposit>()
    }
}
