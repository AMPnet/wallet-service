package com.ampnet.walletservice.controller

import com.ampnet.crowdfunding.proto.TransactionsResponse
import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.controller.pojo.response.BlockchainTransactionsResponse
import com.ampnet.walletservice.controller.pojo.response.PortfolioResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectWithInvestments
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.grpc.blockchain.pojo.BlockchainTransaction
import com.ampnet.walletservice.grpc.blockchain.pojo.Portfolio
import com.ampnet.walletservice.grpc.blockchain.pojo.PortfolioData
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.ampnet.walletservice.service.pojo.PortfolioStats
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class PortfolioControllerTest : ControllerTestBase() {

    private val portfolioPath = "/portfolio"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetMyPortfolio() {
        suppose("User has wallet") {
            createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Project has wallet") {
            createWalletForProject(projectUuid, "1-project-wallet-hash")
            createWalletForProject(testContext.secondProject, "2-project-wallet-hash")
        }
        suppose("Blockchain service will return portfolio") {
            testContext.portfolio = Portfolio(
                listOf(
                    PortfolioData(getWalletHash(projectUuid), 10_000_00),
                    PortfolioData(getWalletHash(testContext.secondProject), 50_000_00)
                )
            )
            Mockito.`when`(
                blockchainService.getPortfolio(getWalletHash(userUuid))
            ).thenReturn(testContext.portfolio)
        }
        suppose("Project service will return projects") {
            Mockito.`when`(
                projectService.getProjects(listOf(projectUuid, testContext.secondProject))
            ).thenReturn(
                listOf(
                    getProjectResponse(projectUuid, userUuid, UUID.randomUUID()),
                    getProjectResponse(testContext.secondProject, userUuid, UUID.randomUUID())
                )
            )
        }

        verify("User can get my portfolio") {
            val result = mockMvc.perform(get(portfolioPath))
                .andExpect(status().isOk)
                .andReturn()

            val portfolioResponse: PortfolioResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(portfolioResponse.portfolio).hasSize(2)
            val project = portfolioResponse.portfolio.first()
            assertThat(project.project.uuid).isEqualTo(projectUuid.toString())
            assertThat(project.investment).isEqualTo(testContext.portfolio.data.first().amount)

            val secondProject = portfolioResponse.portfolio[1]
            assertThat(secondProject.project.uuid).isEqualTo(testContext.secondProject.toString())
            assertThat(secondProject.investment).isEqualTo(testContext.portfolio.data[1].amount)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetMyPortfolioStats() {
        suppose("User has wallet") {
            createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Blockchain service will return portfolio stats") {
            val walletHash = getWalletHash(userUuid)
            val now = ZonedDateTime.now()
            testContext.transactions = listOf(
                BlockchainTransaction(
                    walletHash, "to", 1000,
                    TransactionsResponse.Transaction.Type.INVEST, now.minusYears(1), "MINED"
                ),
                BlockchainTransaction(
                    walletHash, "to_2", 1000,
                    TransactionsResponse.Transaction.Type.INVEST, now.minusMonths(1), "MINED"
                ),
                BlockchainTransaction(
                    "from", walletHash, 10,
                    TransactionsResponse.Transaction.Type.SHARE_PAYOUT, now.minusDays(1), "MINED"
                ),
                BlockchainTransaction(
                    "from_2", walletHash, 10,
                    TransactionsResponse.Transaction.Type.SHARE_PAYOUT, now, "MINED"
                )
            )
            Mockito.`when`(
                blockchainService.getTransactions(getWalletHash(userUuid))
            ).thenReturn(testContext.transactions)
        }

        verify("User can get portfolio stats") {
            val result = mockMvc.perform(get("$portfolioPath/stats"))
                .andExpect(status().isOk)
                .andReturn()

            val stats: PortfolioStats = objectMapper.readValue(result.response.contentAsString)
            assertThat(stats.investments).isEqualTo(2000)
            assertThat(stats.earnings).isEqualTo(20)
            assertThat(stats.dateOfFirstInvestment).isEqualTo(testContext.transactions.first().date)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetEmptyPortfolioStats() {
        suppose("User has wallet") {
            createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Blockchain service will return empty portfolio stats") {
            Mockito.`when`(
                blockchainService.getTransactions(getWalletHash(userUuid))
            ).thenReturn(emptyList())
        }

        verify("User can get empty portfolio stats") {
            val result = mockMvc.perform(get("$portfolioPath/stats"))
                .andExpect(status().isOk)
                .andReturn()

            val stats: PortfolioStats = objectMapper.readValue(result.response.contentAsString)
            assertThat(stats.investments).isEqualTo(0)
            assertThat(stats.earnings).isEqualTo(0)
            assertThat(stats.dateOfFirstInvestment).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetInvestmentsInProject() {
        suppose("User has wallet") {
            createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Project has wallet") {
            createWalletForProject(projectUuid, "project-wallet-hash")
        }
        suppose("Blockchain service will return investments in project") {
            testContext.transactions = listOf(
                createInvestmentInProject(10_000_00),
                createInvestmentInProject(5000_00)
            )
            Mockito.`when`(
                blockchainService.getInvestmentsInProject(
                    getWalletHash(userUuid), getWalletHash(projectUuid)
                )
            ).thenReturn(testContext.transactions)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                projectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }

        verify("User can get a list of investments in project") {
            val result = mockMvc.perform(get("$portfolioPath/project/$projectUuid"))
                .andExpect(status().isOk)
                .andReturn()

            val projectWithInvestments: ProjectWithInvestments = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectWithInvestments.project.uuid).isEqualTo(projectUuid.toString())
            assertThat(projectWithInvestments.transactions.map { it.amount }).hasSize(2)
                .containsAll(testContext.transactions.map { it.amount })
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetNotFoundForInvestmentsInNonExistingProject() {
        verify("Controller will return not found for portfolio in non existing project") {
            val response = mockMvc.perform(get("$portfolioPath/project/${UUID.randomUUID()}"))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetBlockchainTransactions() {
        suppose("User has wallet") {
            createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Project has a wallet") {
            createWalletForProject(projectUuid, "project-wallet-hash")
        }
        suppose("Blockchain service will return portfolio stats") {
            val userWalletHash = getWalletHash(userUuid)
            val projectWalletHash = getWalletHash(projectUuid)
            val now = ZonedDateTime.now()
            testContext.transactions = listOf(
                BlockchainTransaction(
                    "mint", userWalletHash, 1000,
                    TransactionsResponse.Transaction.Type.DEPOSIT, now.minusYears(1), "MINED"
                ),
                BlockchainTransaction(
                    userWalletHash, projectWalletHash, 1000,
                    TransactionsResponse.Transaction.Type.INVEST, now.minusMonths(1), "MINED"
                ),
                BlockchainTransaction(
                    projectWalletHash, userWalletHash, 1000,
                    TransactionsResponse.Transaction.Type.CANCEL_INVESTMENT, now.minusDays(1), "MINED"
                ),
                BlockchainTransaction(
                    projectWalletHash, userWalletHash, 10,
                    TransactionsResponse.Transaction.Type.SHARE_PAYOUT, now.minusDays(1), "MINED"
                ),
                BlockchainTransaction(
                    userWalletHash, "burn", 10,
                    TransactionsResponse.Transaction.Type.WITHDRAW, now, "MINED"
                )
            )
            Mockito.`when`(
                blockchainService.getTransactions(getWalletHash(userUuid))
            ).thenReturn(testContext.transactions)
        }
        suppose("User service will return a list of users") {
            testContext.users = listOf(createUserResponse(userUuid))
            Mockito.`when`(userService.getUsers(setOf(userUuid, projectUuid)))
                .thenReturn(testContext.users)
        }
        suppose("Project service will return a list of projects") {
            testContext.projects = listOf(createProjectResponse(projectUuid))
            Mockito.`when`(projectService.getProjects(setOf(userUuid, projectUuid)))
                .thenReturn(testContext.projects)
        }

        verify("User can get portfolio stats") {
            val result = mockMvc.perform(get("$portfolioPath/transactions"))
                .andExpect(status().isOk)
                .andReturn()

            val user = testContext.users.first()
            val project = testContext.projects.first()
            val response: BlockchainTransactionsResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.transactions).hasSize(5)

            val responseDeposit = response.transactions.first { it.type == TransactionsResponse.Transaction.Type.DEPOSIT }
            assertThat(responseDeposit.from).isNull()
            assertThat(responseDeposit.to).isEqualTo("${user.firstName} ${user.lastName}")
            val responseInvest = response.transactions.first { it.type == TransactionsResponse.Transaction.Type.INVEST }
            assertThat(responseInvest.from).isEqualTo("${user.firstName} ${user.lastName}")
            assertThat(responseInvest.to).isEqualTo(project.name)
            val responseCancelInvestment = response.transactions.first { it.type == TransactionsResponse.Transaction.Type.CANCEL_INVESTMENT }
            assertThat(responseCancelInvestment.from).isEqualTo(project.name)
            assertThat(responseCancelInvestment.to).isEqualTo("${user.firstName} ${user.lastName}")
            val responseSharePayout = response.transactions.first { it.type == TransactionsResponse.Transaction.Type.SHARE_PAYOUT }
            assertThat(responseSharePayout.from).isEqualTo(project.name)
            assertThat(responseSharePayout.to).isEqualTo("${user.firstName} ${user.lastName}")
            val responseWithdraw = response.transactions.first { it.type == TransactionsResponse.Transaction.Type.WITHDRAW }
            assertThat(responseWithdraw.from).isEqualTo("${user.firstName} ${user.lastName}")
            assertThat(responseWithdraw.to).isNull()
        }
    }

    private fun createInvestmentInProject(amount: Long): BlockchainTransaction =
        BlockchainTransaction(
            getWalletHash(userUuid),
            getWalletHash(projectUuid), amount,
            TransactionsResponse.Transaction.Type.INVEST,
            ZonedDateTime.now(), "MINED"
        )

    private class TestContext {
        val secondProject: UUID = UUID.randomUUID()
        lateinit var portfolio: Portfolio
        lateinit var transactions: List<BlockchainTransaction>
        lateinit var users: List<UserResponse>
        lateinit var projects: List<ProjectResponse>
    }
}
