package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class InvestmentControllerTest : ControllerTestBase() {

    private val projectInvestmentPath = "/invest/project"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGenerateProjectInvestmentTransaction() {
        suppose("Project has empty wallet") {
            val projectWallet = createWalletForProject(projectUuid, "project-wallet")
            Mockito.`when`(blockchainService.getBalance(getWalletHash(projectWallet))).thenReturn(0)
        }
        suppose("User has wallet") {
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User does have enough funds on wallet") {
            val userWalletHash = getWalletHash(userUuid)
            Mockito.`when`(blockchainService.getBalance(userWalletHash)).thenReturn(100_000_00)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                projectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }
        suppose("Blockchain service will generate invest transaction") {
            val userWalletHash = getWalletHash(userUuid)
            val projectWalletHash = getWalletHash(projectUuid)
            Mockito.`when`(
                blockchainService.generateProjectInvestmentTransaction(
                    ProjectInvestmentTxRequest(userWalletHash, projectWalletHash, testContext.investment)
                )
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate invest project transaction") {
            val request = AmountRequest(testContext.investment)
            val result = mockMvc.perform(
                post("$projectInvestmentPath/$projectUuid")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.INVEST)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGenerateCancelInvestmentsInProjectTransaction() {
        suppose("Project has empty wallet") {
            val projectWallet = createWalletForProject(projectUuid, "project-wallet")
            Mockito.`when`(blockchainService.getBalance(getWalletHash(projectWallet))).thenReturn(0)
        }
        suppose("User has wallet") {
            createWalletForUser(userUuid, walletHash)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                projectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }
        suppose("Blockchain service will generate cancel investments transaction") {
            val userWalletHash = getWalletHash(userUuid)
            val projectWalletHash = getWalletHash(projectUuid)
            Mockito.`when`(
                blockchainService.generateCancelInvestmentsInProject(userWalletHash, projectWalletHash)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate cancel invests in project transaction") {
            val result = mockMvc.perform(
                post("$projectInvestmentPath/$projectUuid/cancel")
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.CANCEL_INVEST)
        }
    }

    private class TestContext {
        val transactionData = TransactionData("data")
        val investment = 100_00L
    }
}
