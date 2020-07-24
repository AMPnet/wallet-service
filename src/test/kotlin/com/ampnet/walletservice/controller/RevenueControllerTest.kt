package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.RevenuePayoutsResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.grpc.blockchain.pojo.RevenuePayoutTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.RevenuePayout
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class RevenueControllerTest : ControllerTestBase() {

    private val revenuePath = "/revenue"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllRevenuePayouts()
        databaseCleanerService.deleteAllWallets()
        databaseCleanerService.deleteAllTransactionInfo()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGenerateRevenuePayoutTransaction() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, walletHash)
        }
        suppose("Project has a wallet with enough funds") {
            val projectWallet = createWalletForProject(projectUuid, "project-wallet")
            Mockito.`when`(blockchainService.getBalance(getWalletHash(projectWallet)))
                .thenReturn(testContext.amount)
        }
        suppose("Blockchain service will generate revenue payout transaction") {
            val userWalletHash = getWalletHash(userUuid)
            val projectWalletHash = getWalletHash(projectUuid)
            val request = RevenuePayoutTxRequest(userWalletHash, projectWalletHash, testContext.amount)
            Mockito.`when`(blockchainService.generateRevenuePayout(request))
                .thenReturn(testContext.transactionData)
        }
        suppose("Project service will return project for user") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }

        verify("User can create revenue payout") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                post("$revenuePath/payout/project/$projectUuid")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.REVENUE_PAYOUT)
        }
        verify("Transaction info for revenue payout is created") {
            val txInfo = transactionInfoRepository.findAll()[0]
            assertThat(txInfo.companionData).isNotNull()
            assertThat(txInfo.type).isEqualTo(TransactionType.REVENUE_PAYOUT)
            assertThat(txInfo.userUuid).isEqualTo(userUuid)
        }
        verify("Revenue payout is created") {
            val revenuePayout = revenuePayoutRepository.findAll()[0]
            assertThat(revenuePayout.amount).isEqualTo(testContext.amount)
            assertThat(revenuePayout.projectUuid).isEqualTo(projectUuid)
            assertThat(revenuePayout.createdBy).isEqualTo(userUuid)
            assertThat(revenuePayout.txHash).isNull()
            assertThat(revenuePayout.completedAt).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetRevenuePayouts() {
        suppose("Project has revenue payouts") {
            revenuePayoutRepository.save(RevenuePayout(projectUuid, 100L, userUuid))
            revenuePayoutRepository.save(RevenuePayout(projectUuid, 55L, userUuid))
        }

        verify("User can get revenue payouts") {
            val result = mockMvc.perform(
                get("$revenuePath/payout/project/$projectUuid")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val payouts: RevenuePayoutsResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(payouts.page).isEqualTo(0)
            assertThat(payouts.totalPages).isEqualTo(1)
            assertThat(payouts.revenuePayouts.map { it.amount }).containsAll(listOf(100L, 55L))
        }
    }

    private class TestContext {
        val transactionData = TransactionData("data")
        val amount = 100_00L
    }
}
