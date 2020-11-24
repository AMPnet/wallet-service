package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WalletTransferRequest
import com.ampnet.walletservice.controller.pojo.response.OrganizationWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.UserWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.PrivilegeType
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.enums.TransferWalletType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class CooperativeWalletControllerTest : ControllerTestBase() {

    private val cooperativeWalletPath = "/cooperative/wallet"
    private val createdAtDesc = "createdAt,desc"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        databaseCleanerService.deleteAllTransactionInfo()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WALLET])
    fun mustBeAbleToGetActivateWalletTransaction() {
        suppose("Unactivated user created wallet") {
            testContext.wallet = createUnactivatedWallet(userUuid, testContext.activationData, WalletType.USER)
        }
        suppose("Blockchain service will return transaction data for activating user wallet") {
            testContext.transactionData = TransactionData(testContext.walletHash)
            Mockito.`when`(
                blockchainService.addWallet(testContext.activationData, COOP)
            ).thenReturn(testContext.transactionData)
        }

        verify("Cooperative can generate activate wallet transaction") {
            val result = mockMvc.perform(
                post("$cooperativeWalletPath/${testContext.wallet.uuid}/transaction")
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.WALLET_ACTIVATE)
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WALLET])
    fun mustBeAbleToGetUsersWithUnactivatedWallet() {
        suppose("There are users with unactivated wallets") {
            val user = UUID.randomUUID()
            createUnactivatedWallet(user, "activation-data-1", WalletType.USER)
            testContext.users.add(user)
            val secondUser = UUID.randomUUID()
            createUnactivatedWallet(secondUser, "activation-data-2", WalletType.USER)
            testContext.users.add(secondUser)
        }
        suppose("There is user with activated wallet") {
            testContext.wallet = createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("There is user with unactivated wallet from another coop") {
            val thirdUser = UUID.randomUUID()
            testContext.anotherCoopWallet = createUnactivatedWallet(
                thirdUser, "activation-data-3", WalletType.USER, anotherCoop
            )
        }
        suppose("User service will return data for users") {
            Mockito.`when`(
                userService.getUsers(testContext.users.toSet())
            ).thenReturn(listOf(createUserResponse(testContext.users[0]), createUserResponse(testContext.users[1])))
            Mockito.`when`(
                userService.getUsers(testContext.users.reversed().toSet())
            ).thenReturn(listOf(createUserResponse(testContext.users[1]), createUserResponse(testContext.users[0])))
        }

        verify("Cooperative can get a list of users with unactivated wallet") {
            val result = mockMvc.perform(
                get("$cooperativeWalletPath/user")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", createdAtDesc)
            )
                .andExpect(status().isOk)
                .andReturn()

            val userListResponse: UserWithWalletListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userListResponse.users).hasSize(2)
            userListResponse.users.forEach { assertThat(it.wallet.coop).isEqualTo(COOP) }
            assertThat(userListResponse.users.map { it.user.uuid })
                .containsAll(testContext.users)
                .doesNotContain(userUuid)
            assertThat(userListResponse.users.map { it.wallet })
                .doesNotContain(WalletResponse(testContext.wallet, 0))
            assertThat(userListResponse.users.map { it.wallet })
                .doesNotContain(WalletResponse(testContext.anotherCoopWallet, 0))
            assertThat(userListResponse.page).isEqualTo(0)
            assertThat(userListResponse.totalPages).isEqualTo(1)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WALLET])
    fun mustBeAbleToGetOrganizationsWithUnactivatedWallet() {
        suppose("There are organizations with unactivated wallets") {
            val organization = UUID.randomUUID()
            createUnactivatedWallet(organization, "org-1", WalletType.ORG)
            testContext.organizations.add(organization)
            val secondOrg = UUID.randomUUID()
            createUnactivatedWallet(secondOrg, "org-2", WalletType.ORG)
            testContext.organizations.add(secondOrg)
        }
        suppose("There is organization with activated wallet") {
            testContext.wallet = createWalletForOrganization(organizationUuid, testContext.walletHash)
        }
        suppose("There is organization with unactivated wallet from another coop") {
            val thirdOrg = UUID.randomUUID()
            testContext.anotherCoopWallet = createUnactivatedWallet(
                thirdOrg, "org-3", WalletType.ORG, anotherCoop
            )
        }
        suppose("Project service will return organizations data") {
            Mockito.`when`(
                projectService.getOrganizations(testContext.organizations.toSet())
            ).thenReturn(
                listOf(
                    getOrganizationResponse(testContext.organizations[0], userUuid),
                    getOrganizationResponse(testContext.organizations[1], userUuid)
                )
            )
            Mockito.`when`(
                projectService.getOrganizations(testContext.organizations.reversed().toSet())
            ).thenReturn(
                listOf(
                    getOrganizationResponse(testContext.organizations[1], userUuid),
                    getOrganizationResponse(testContext.organizations[0], userUuid)
                )
            )
        }

        verify("Cooperative can get a list of organization with unactivated wallet") {
            val result = mockMvc.perform(
                get("$cooperativeWalletPath/organization")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", createdAtDesc)
            )
                .andExpect(status().isOk)
                .andReturn()

            val orgListResponse: OrganizationWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(orgListResponse.organizations).hasSize(2)
            orgListResponse.organizations.forEach { assertThat(it.wallet.coop).isEqualTo(COOP) }
            assertThat(orgListResponse.organizations.map { it.organization.uuid })
                .containsAll(testContext.organizations.map { it.toString() })
            assertThat(orgListResponse.organizations.map { it.wallet.activationData })
                .doesNotContain(testContext.wallet.hash)
            assertThat(orgListResponse.organizations.map { it.wallet.activationData })
                .doesNotContain(testContext.anotherCoopWallet.hash)
            assertThat(orgListResponse.page).isEqualTo(0)
            assertThat(orgListResponse.totalPages).isEqualTo(1)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WALLET])
    fun mustBeAbleToGetProjectsWithUnactivatedWallet() {
        suppose("There are projects with unactivated wallets") {
            val project = UUID.randomUUID()
            createUnactivatedWallet(project, "project-1", WalletType.PROJECT)
            testContext.projects.add(project)
            val secondProject = UUID.randomUUID()
            createUnactivatedWallet(secondProject, "project-2", WalletType.PROJECT)
            testContext.projects.add(secondProject)
        }
        suppose("There is projects with activated wallet") {
            testContext.wallet = createWalletForProject(projectUuid, testContext.walletHash)
        }
        suppose("There is project with unactivated wallet from another coop") {
            val thirdProject = UUID.randomUUID()
            testContext.anotherCoopWallet = createUnactivatedWallet(
                thirdProject, "project-3", WalletType.PROJECT, anotherCoop
            )
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

        verify("Cooperative can get a list of projects with unactivated wallet") {
            val result = mockMvc.perform(
                get("$cooperativeWalletPath/project")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", createdAtDesc)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectListResponse: ProjectWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(projectListResponse.projects).hasSize(2)
            projectListResponse.projects.forEach { assertThat(it.wallet.coop).isEqualTo(COOP) }
            assertThat(projectListResponse.projects.map { it.project.uuid })
                .containsAll(testContext.projects)
            assertThat(projectListResponse.projects.map { it.wallet.activationData })
                .doesNotContain(testContext.wallet.hash)
            assertThat(projectListResponse.projects.map { it.wallet.activationData })
                .doesNotContain(testContext.anotherCoopWallet.hash)
            assertThat(projectListResponse.page).isEqualTo(0)
            assertThat(projectListResponse.totalPages).isEqualTo(1)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WALLET_TRANSFER])
    fun mustBeAbleToTransferTokenIssuer() {
        suppose("There is a user with activated wallet") {
            testContext.userUuid = UUID.randomUUID()
            testContext.wallet = createWalletForUser(testContext.userUuid, testContext.walletHash)
        }
        suppose("Blockchain service will return transaction data for transferring token issuer") {
            testContext.transactionData = TransactionData(testContext.walletHash)
            Mockito.`when`(
                blockchainService.generateTransferTokenIssuer(testContext.wallet.activationData, COOP)
            ).thenReturn(testContext.transactionData)
        }

        verify("Admin can transfer wallet ownership to user") {
            val request = WalletTransferRequest(testContext.userUuid, TransferWalletType.TOKEN_ISSUER)
            val result = mockMvc.perform(
                post("$cooperativeWalletPath/transfer/transaction")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.TRNSF_TOKEN_OWN)
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
        verify("Transaction info for transfer token issuer is created") {
            val transactionInfo = transactionInfoRepository.findAll().first()
            assertThat(transactionInfo.type).isEqualTo(TransactionType.TRNSF_TOKEN_OWN)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
            assertThat(transactionInfo.companionData).isEqualTo(testContext.userUuid.toString())
            assertThat(transactionInfo.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WALLET_TRANSFER])
    fun mustBeAbleToTransferPlatformOwner() {
        suppose("There is a user with activated wallet") {
            testContext.userUuid = UUID.randomUUID()
            testContext.wallet = createWalletForUser(testContext.userUuid, testContext.walletHash)
        }
        suppose("Blockchain service will return transaction data for transferring platform owner") {
            testContext.transactionData = TransactionData(testContext.walletHash)
            Mockito.`when`(
                blockchainService.generateTransferPlatformManager(testContext.wallet.activationData, COOP)
            ).thenReturn(testContext.transactionData)
        }

        verify("Admin can transfer wallet ownership to user") {
            val request = WalletTransferRequest(testContext.userUuid, TransferWalletType.PLATFORM_MANAGER)
            val result = mockMvc.perform(
                post("$cooperativeWalletPath/transfer/transaction")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.TRNSF_PLTFRM_OWN)
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
        verify("Transaction info for transfer platform owner is created") {
            val transactionInfo = transactionInfoRepository.findAll().first()
            assertThat(transactionInfo.type).isEqualTo(TransactionType.TRNSF_PLTFRM_OWN)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
            assertThat(transactionInfo.companionData).isEqualTo(testContext.userUuid.toString())
            assertThat(transactionInfo.coop).isEqualTo(COOP)
        }
    }

    private fun createUnactivatedWallet(owner: UUID, activationData: String, type: WalletType, coop: String = COOP): Wallet {
        val wallet = Wallet(owner, activationData, type, Currency.EUR, coop)
        return walletRepository.save(wallet)
    }

    private class TestContext {
        val activationData = "th_HKYbpdgc8yhGvMaEmpk2KK9AXE3yz8kf5imyv52XVwcnqZKei"
        val walletHash = "th_R26wx2hTnhmgDKJhXC9GAH3evCRnTyyXg4fivLLEAyiAcVW2K"
        val users = mutableListOf<UUID>()
        val organizations = mutableListOf<UUID>()
        val projects = mutableListOf<UUID>()
        lateinit var userUuid: UUID
        lateinit var transactionData: TransactionData
        lateinit var wallet: Wallet
        lateinit var anotherCoopWallet: Wallet
    }
}
