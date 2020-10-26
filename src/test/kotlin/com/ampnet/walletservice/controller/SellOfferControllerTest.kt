package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.ProjectsWithSellOffersResponse
import com.ampnet.walletservice.grpc.blockchain.pojo.CounterOfferData
import com.ampnet.walletservice.grpc.blockchain.pojo.SellOfferData
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class SellOfferControllerTest : ControllerTestBase() {

    private val salesPath = "/sell"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetSellOffers() {
        suppose("There are projects with activated wallets") {
            val project = UUID.randomUUID()
            testContext.projectWallet = createWalletForProject(project, testContext.walletHash)
            testContext.projects.add(project)
            val secondProject = UUID.randomUUID()
            testContext.secondProjectWallet = createWalletForProject(secondProject, testContext.walletHash2)
            testContext.projects.add(secondProject)
        }
        suppose("Project service will return projects data") {
            Mockito.`when`(
                projectService.getProjects(testContext.projects.toSet())
            ).thenReturn(
                listOf(
                    getProjectResponse(testContext.projects[0], userUuid, UUID.randomUUID()),
                    getProjectResponse(testContext.projects[1], userUuid, UUID.randomUUID())
                )
            )
            Mockito.`when`(
                projectService.getProjects(testContext.projects.reversed().toSet())
            ).thenReturn(
                listOf(
                    getProjectResponse(testContext.projects[1], userUuid, UUID.randomUUID()),
                    getProjectResponse(testContext.projects[0], userUuid, UUID.randomUUID())
                )
            )
        }
        suppose("Blockchain service will return sell offers") {
            Mockito.`when`(
                blockchainService.getSellOffers()
            ).thenReturn(
                listOf(
                    createSellOffer(testContext.walletHash, 1, 1),
                    createSellOffer(testContext.walletHash, 11, 11),
                    createSellOffer(testContext.walletHash2, 2, 2),
                    createSellOffer(testContext.walletHash2, 22, 22),
                    createSellOffer(walletHash, 3, 33)
                )
            )
        }

        verify("User can get sell offers") {
            val result = mockMvc.perform(MockMvcRequestBuilders.get("$salesPath/offer"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val response: ProjectsWithSellOffersResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.projects).hasSize(2)
            assertThat(response.coop).isEqualTo(COOP)
            val projectWithSellOffer = response.projects
                .first { it.project.uuid == testContext.projectWallet.owner }
            assertThat(projectWithSellOffer.sellOffers).hasSize(2)
            assertThat(projectWithSellOffer.sellOffers.map { it.price }).contains(1, 11)
            assertThat(projectWithSellOffer.sellOffers[0].counterOffers).hasSize(1)

            val secondProjectWithWallet = response.projects
                .first { it.project.uuid == testContext.secondProjectWallet.owner }
            assertThat(secondProjectWithWallet.sellOffers).hasSize(2)
            assertThat(secondProjectWithWallet.sellOffers.map { it.price }).contains(2, 22)
        }
    }

    private fun createSellOffer(projectWalletHash: String, shares: Long, price: Long) =
        SellOfferData(projectWalletHash, walletHash, shares, price, listOf(CounterOfferData(walletHash, 1)))

    private class TestContext {
        lateinit var projectWallet: Wallet
        lateinit var secondProjectWallet: Wallet
        val projects = mutableListOf<UUID>()
        val walletHash = "th_HKYbpdgc8yhGvMaEmpk2KK9AXE3yz8kf5imyv52XVwcnqZKei"
        val walletHash2 = "th_R26wx2hTnhmgDKJhXC9GAH3evCRnTyyXg4fivLLEAyiAcVW2K"
    }
}
