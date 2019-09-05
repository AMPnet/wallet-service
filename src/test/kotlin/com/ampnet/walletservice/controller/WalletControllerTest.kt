package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WalletCreateRequest
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.grpc.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.controller.pojo.response.PairWalletResponse
import com.ampnet.walletservice.persistence.model.PairWalletCode
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class WalletControllerTest : ControllerTestBase() {

    private val walletPath = "/wallet"
    private val projectWalletPath = "/wallet/project"
    private val organizationWalletPath = "/wallet/organization"

    private lateinit var testData: TestData

    @BeforeEach
    fun initTestData() {
        databaseCleanerService.deleteAllWallets()
        testData = TestData()
    }

    /* User Wallet */
    @Test
    fun mustBeAbleToGeneratePairWalletCode() {
        suppose("User did not create pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
        }

        verify("User can generate pair wallet code") {
            val request = WalletCreateRequest(testData.publicKey)
            val result = mockMvc.perform(
                    post("$walletPath/pair")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val pairWalletResponse: PairWalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(pairWalletResponse.code).isNotEmpty()
            assertThat(pairWalletResponse.publicKey).isEqualTo(request.publicKey)
            testData.pairWalletCode = pairWalletResponse.code
        }
        verify("Pair wallet code is stored") {
            val optionalPairWalletCode = pairWalletCodeRepository.findByPublicKey(testData.publicKey)
            assertThat(optionalPairWalletCode).isPresent
            val pairWalletCode = optionalPairWalletCode.get()
            assertThat(pairWalletCode.code).isEqualTo(testData.pairWalletCode)
            assertThat(pairWalletCode.publicKey).isEqualTo(testData.publicKey)
            assertThat(pairWalletCode.createdAt).isBefore(ZonedDateTime.now())
        }
    }

    @Test
    fun mustReturnNotFoundForNonExistingPairWalletCode() {
        suppose("Pair wallet is missing") {
            databaseCleanerService.deleteAllPairWalletCodes()
        }

        verify("User will get not found for non existing pair wallet code") {
            mockMvc.perform(get("$walletPath/pair/000000"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustReturnPairWalletCode() {
        suppose("User did create pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
            testData.pairWalletCode = "N4CD12"
            val pairWalletCode = PairWalletCode(0, testData.publicKey, testData.pairWalletCode, ZonedDateTime.now())
            pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("User can pair wallet code") {
            val result = mockMvc.perform(get("$walletPath/pair/${testData.pairWalletCode}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val pairWalletResponse: PairWalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(pairWalletResponse.code).isEqualTo(testData.pairWalletCode)
            assertThat(pairWalletResponse.publicKey).isEqualTo(testData.publicKey)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOwnWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(userUuid, testData.hash)
        }
        suppose("User has some funds on wallet") {
            testData.balance = 100_00
            Mockito.`when`(blockchainService.getBalance(testData.hash)).thenReturn(testData.balance)
        }

        verify("Controller returns user wallet response") {
            val result = mockMvc.perform(get(walletPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.uuid).isEqualTo(testData.wallet.uuid)
            assertThat(walletResponse.hash).isEqualTo(testData.hash)
            assertThat(walletResponse.currency).isEqualTo(testData.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testData.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            assertThat(walletResponse.balance).isEqualTo(testData.balance)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForMissingWallet() {
        verify("Controller returns 404 for missing wallet") {
            mockMvc.perform(get(walletPath))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateWallet() {
        verify("User can create a wallet") {
            val request = WalletCreateRequest(testData.publicKey)
            val result = mockMvc.perform(
                    post(walletPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.uuid).isNotNull()
            assertThat(walletResponse.activationData).isEqualTo(testData.publicKey)
            assertThat(walletResponse.currency).isEqualTo(Currency.EUR)
            assertThat(walletResponse.type).isEqualTo(WalletType.USER)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(walletResponse.hash).isNull()
            assertThat(walletResponse.activatedAt).isNull()

            testData.walletUuid = walletResponse.uuid
        }
        verify("Wallet is created") {
            val userWallet = walletRepository.findByOwner(userUuid)
            assertThat(userWallet).isPresent
            val wallet = userWallet.get()
            assertThat(wallet.activationData).isEqualTo(testData.publicKey)
            assertThat(wallet.hash).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateAdditionalWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(userUuid, testData.publicKey)
        }

        verify("User cannot create a wallet") {
            val request = WalletCreateRequest(testData.publicKey)
            mockMvc.perform(
                    post(walletPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
        }
    }

    /* Project Wallet */
    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetCreateProjectWalletTransaction() {
        suppose("User has a wallet") {
            testData.wallet = createWalletForUser(userUuid, testData.hash)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(organizationUuid, testData.hash2)
        }
        suppose("Blockchain service successfully generates transaction to create project wallet") {
            val orgWalletHash = getWalletHash(organizationUuid)
            val userWalletHash = getWalletHash(userUuid)
            testData.transactionData = TransactionData(testData.signedTransaction)
            testData.time = ZonedDateTime.now().plusDays(30)
            val request = GenerateProjectWalletRequest(userWalletHash, orgWalletHash, 100000, 100, 10000000,
                testData.time.toInstant().toEpochMilli())
            Mockito.`when`(
                    blockchainService.generateProjectWalletTransaction(request)
            ).thenReturn(testData.transactionData)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                projectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid, endDate = testData.time))
        }

        verify("User can get transaction to sign") {
            val result = mockMvc.perform(
                    get("$projectWalletPath/$projectUuid/transaction"))
                    .andExpect(status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testData.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.CREATE_PROJECT)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToGetCreateProjectWalletTransactionIfWalletExits() {
        suppose("Project wallet exists") {
            testData.wallet = createWalletForProject(projectUuid, testData.hash)
        }

        verify("User cannot get create wallet transaction") {
            val response = mockMvc.perform(
                    get("$projectWalletPath/$projectUuid/transaction"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.WALLET_EXISTS)
        }
    }

    /* Organization Wallet */
    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOrganizationWallet() {
        suppose("Organization has a wallet") {
            testData.wallet = createWalletForOrganization(organizationUuid, testData.hash)
        }

        verify("User can fetch organization wallet") {
            val result = mockMvc.perform(
                    get("$organizationWalletPath/$organizationUuid"))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.uuid).isEqualTo(testData.wallet.uuid)
            assertThat(walletResponse.hash).isEqualTo(testData.hash)
            assertThat(walletResponse.currency).isEqualTo(testData.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testData.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForMissingOrganizationWallet() {
        verify("Controller will return not found") {
            mockMvc.perform(get("$organizationWalletPath/${UUID.randomUUID()}"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetCreateOrganizationWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(userUuid, testData.publicKey)
        }
        suppose("Blockchain service successfully creates organization") {
            testData.transactionData = TransactionData(testData.signedTransaction)
            Mockito.`when`(
                blockchainService.generateCreateOrganizationTransaction(getWalletHash(userUuid))
            ).thenReturn(testData.transactionData)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                projectService.getOrganization(organizationUuid)
            ).thenReturn(getOrganizationResponse(organizationUuid, userUuid))
        }

        verify("User can get transaction create organization wallet") {
            val path = "$organizationWalletPath/$organizationUuid/transaction"
            val result = mockMvc.perform(
                    get(path))
                    .andExpect(status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testData.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.CREATE_ORG)
        }
    }

    private class TestData {
        lateinit var wallet: Wallet
        lateinit var transactionData: TransactionData
        lateinit var walletUuid: UUID
        var hash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
        var hash2 = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7893"
        val publicKey = "0xC2D7CF95645D33006175B78989035C7c9061d3F9"
        var balance: Long = -1
        val signedTransaction = "SignedTransaction"
        lateinit var pairWalletCode: String
        lateinit var time: ZonedDateTime
        lateinit var uuid: UUID
    }
}
