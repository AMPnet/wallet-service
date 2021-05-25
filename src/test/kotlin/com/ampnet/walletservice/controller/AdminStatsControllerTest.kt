package com.ampnet.walletservice.controller

import com.ampnet.crowdfunding.proto.UserWalletsForCoopAndTxTypeResponse
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.PrivilegeType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.ampnet.walletservice.service.pojo.response.StatsResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class AdminStatsControllerTest : ControllerTestBase() {

    private val statsPath = "/admin/stats"

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        databaseCleanerService.deleteAllDeposits()
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetStats() {
        suppose("There is unapproved deposit") {
            createUnsignedDeposit(UUID.randomUUID())
        }
        suppose("There is approved deposit") {
            createApprovedDeposit(UUID.randomUUID())
        }
        suppose("There are approved deposits for the same user") {
            val user = UUID.randomUUID()
            createApprovedDeposit(user)
            createApprovedDeposit(user)
            createApprovedDeposit(user)
        }
        suppose("There is approved deposit from another coop") {
            createApprovedDeposit(UUID.randomUUID(), coop = anotherCoop)
        }
        suppose("Some wallets are initialized") {
            createWalletForUser(UUID.randomUUID(), "some-hash")
            createWalletForOrganization(UUID.randomUUID(), "org-hash")
            createWalletForProject(UUID.randomUUID(), "project-hash")
        }
        suppose("There is unapproved user wallet") {
            val wallet = Wallet(UUID.randomUUID(), "activationData", WalletType.USER, Currency.EUR, COOP)
            walletRepository.save(wallet)
        }
        suppose("Blockchain service will return a list of wallets") {
            val response = UserWalletsForCoopAndTxTypeResponse.WalletWithHash.newBuilder()
                .setWallet("activation-data")
                .setWalletTxHash("some-hash")
                .build()
            BDDMockito.given(blockchainService.getUserWalletsWithInvestment(COOP))
                .willReturn(listOf(response))
        }

        verify("Cooperative user can get statistics about counted users with approved deposit") {
            val result = mockMvc.perform(MockMvcRequestBuilders.get(statsPath))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val stats: StatsResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(stats.usersDeposited).isEqualTo(2)
            assertThat(stats.walletsInitialized).isEqualTo(2)
            assertThat(stats.usersInvested).isEqualTo(1)
        }
    }
}
