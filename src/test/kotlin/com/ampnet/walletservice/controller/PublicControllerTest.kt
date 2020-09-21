package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
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
    private val publicProjectActivePath = "/public/project/active"

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
        }
    }

    @Test
    fun mustReturnNotFoundForMissingProjectWallet() {
        verify("Controller will return not found for missing project wallet") {
            mockMvc.perform(get("$projectWalletPublicPath/${UUID.randomUUID()}"))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustBeAbleToGetAllActiveProjectsWithWallet() {
        suppose("Project wallet exists") {
            testContext.wallet = createWalletForProject(projectUuid, walletHash)
        }
        suppose("Second inactive project wallet exists") {
            testContext.inactiveProjectWallet =
                createWalletForProject(UUID.randomUUID(), "th_49bC6a8219c798394726f8e86E040A878da1d00A")
        }
        suppose("There is project wallet from another cooperative") {
            testContext.anotherCoopWallet =
                createWalletForProject(UUID.randomUUID(), "th_49bC6a8219c798394726f8e86E040A878da1d00B", anotherCoop)
        }
        suppose("Blockchain service will return projects info") {
            val inactiveWalletHash = getWalletHash(testContext.inactiveProjectWallet)
            Mockito.`when`(
                blockchainService.getProjectsInfo(listOf(walletHash, inactiveWalletHash))
            ).thenReturn(
                listOf(
                    getProjectInfoResponse(walletHash, testContext.projectBalance),
                    getProjectInfoResponse(inactiveWalletHash, testContext.projectBalance - 100)
                )
            )
            Mockito.`when`(
                blockchainService.getProjectsInfo(listOf(inactiveWalletHash, walletHash))
            ).thenReturn(
                listOf(
                    getProjectInfoResponse(inactiveWalletHash, testContext.projectBalance - 100),
                    getProjectInfoResponse(walletHash, testContext.projectBalance)
                )
            )
        }
        suppose("Project service will return projects") {
            val projects = listOf(projectUuid, testContext.inactiveProjectWallet.owner)
            Mockito.`when`(
                projectService.getProjects(projects.toSet())
            ).thenReturn(
                listOf(
                    getProjectResponse(projects[0], userUuid, UUID.randomUUID()),
                    getProjectResponse(projects[1], userUuid, UUID.randomUUID(), active = false)
                )
            )
        }

        verify("Controller will return only active project") {
            val result = mockMvc.perform(
                get(publicProjectActivePath)
                    .param("coop", COOP)
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectsResponse: ProjectWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(1)
            val projectWithWallet = projectsResponse.projects.first()
            assertThat(projectWithWallet.project.uuid).isEqualTo(projectUuid.toString())
            assertThat(projectWithWallet.wallet.uuid).isEqualTo(testContext.wallet.uuid)
            assertThat(projectWithWallet.wallet.balance).isEqualTo(testContext.projectBalance)
            assertThat(projectsResponse.page).isEqualTo(0)
            assertThat(projectsResponse.totalPages).isEqualTo(1)
        }
    }

    @Test
    fun mustBeAbleToGetEmptyListOfActiveProjects() {
        verify("Controller will return empty list of active projects") {
            val result = mockMvc.perform(
                get(publicProjectActivePath)
                    .param("coop", COOP)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectsResponse: ProjectWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(0)
        }
    }

    @Test
    fun mustNotReturnEndedProjectInActiveProjects() {
        suppose("Project wallet exists") {
            testContext.wallet = createWalletForProject(projectUuid, walletHash)
        }
        suppose("Blockchain service will return projects info") {
            Mockito.`when`(
                blockchainService.getProjectsInfo(listOf(walletHash))
            ).thenReturn(
                listOf(getProjectInfoResponse(walletHash, testContext.projectBalance))
            )
        }
        suppose("Project service will return projects") {
            Mockito.`when`(
                projectService.getProjects(setOf(projectUuid))
            ).thenReturn(
                listOf(getProjectResponse(projectUuid, userUuid, UUID.randomUUID(), endDate = ZonedDateTime.now().minusDays(10)))
            )
        }

        verify("Controller will not return ended project") {
            val result = mockMvc.perform(
                get(publicProjectActivePath)
                    .param("coop", COOP)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectsResponse: ProjectWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(0)
        }
    }

    @Test
    @WithMockCrowdfoundUser(verified = false)
    fun mustBeAbleToGetActiveProjectsWithUnVerifiedAccount() {
        verify("User with unverified account can access public project path") {
            val result = mockMvc.perform(
                get(publicProjectActivePath)
                    .param("coop", COOP)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectsResponse: ProjectWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(0)
        }
    }

    private class TestContext {
        lateinit var wallet: Wallet
        lateinit var inactiveProjectWallet: Wallet
        lateinit var anotherCoopWallet: Wallet
        val walletBalance = 100L
        val projectBalance = 10_000_000_00L
    }
}
