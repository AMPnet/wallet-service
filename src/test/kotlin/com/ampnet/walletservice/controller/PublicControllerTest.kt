package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.persistence.model.Wallet
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class PublicControllerTest : ControllerTestBase() {

    private val projectWalletPublicPath = "/public/wallet/project"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetProjectWallet() {
        suppose("Project wallet exists") {
            testContext.wallet = createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project wallet has some balance") {
            Mockito.`when`(blockchainService.getBalance(walletHash)).thenReturn(testContext.walletBalance)
        }

        verify("User can get project wallet") {
            val result = mockMvc.perform(get("$projectWalletPublicPath/$projectUuid"))
                .andExpect(status().isOk)
                .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.uuid).isEqualTo(testContext.wallet.uuid)
            assertThat(walletResponse.hash).isEqualTo(walletHash)
            assertThat(walletResponse.currency).isEqualTo(testContext.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testContext.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(walletResponse.balance).isEqualTo(testContext.walletBalance)
            assertThat(walletResponse.coop).isEqualTo(COOP)
        }
    }

    @Test
    fun mustReturnNotFoundForMissingProjectWallet() {
        verify("Controller will return not found for missing project wallet") {
            mockMvc.perform(get("$projectWalletPublicPath/${UUID.randomUUID()}"))
                .andExpect(status().isNotFound)
        }
    }

    private class TestContext {
        lateinit var wallet: Wallet
        val walletBalance = 100L
    }
}
