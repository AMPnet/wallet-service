package com.ampnet.walletservice.controller

import com.ampnet.mailservice.proto.WalletTypeRequest
import com.ampnet.walletservice.controller.pojo.request.WalletCreateRequest
import com.ampnet.walletservice.controller.pojo.request.WalletPairRequest
import com.ampnet.walletservice.controller.pojo.response.PairWalletResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.grpc.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.PairWalletCode
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
import java.time.ZonedDateTime
import java.util.UUID

class WalletControllerTest : ControllerTestBase() {

    private val walletPath = "/wallet"
    private val projectWalletPath = "/wallet/project"
    private val organizationWalletPath = "/wallet/organization"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    /* User Wallet */
    @Test
    fun mustBeAbleToGeneratePairWalletCode() {
        suppose("User did not create pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
        }

        verify("User can generate pair wallet code") {
            val request = WalletPairRequest(testContext.publicKey)
            val result = mockMvc.perform(
                post("$walletPath/pair")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val pairWalletResponse: PairWalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(pairWalletResponse.code).isNotEmpty()
            assertThat(pairWalletResponse.publicKey).isEqualTo(request.publicKey)
            testContext.pairWalletCode = pairWalletResponse.code
        }
        verify("Pair wallet code is stored") {
            val optionalPairWalletCode = pairWalletCodeRepository.findByPublicKey(testContext.publicKey)
            assertThat(optionalPairWalletCode).isPresent
            val pairWalletCode = optionalPairWalletCode.get()
            assertThat(pairWalletCode.code).isEqualTo(testContext.pairWalletCode)
            assertThat(pairWalletCode.publicKey).isEqualTo(testContext.publicKey)
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
            testContext.pairWalletCode = "N4CD12"
            val pairWalletCode = PairWalletCode(0, testContext.publicKey, testContext.pairWalletCode, ZonedDateTime.now())
            pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("User can pair wallet code") {
            val result = mockMvc.perform(
                get("$walletPath/pair/${testContext.pairWalletCode}")
            )
                .andExpect(status().isOk)
                .andReturn()

            val pairWalletResponse: PairWalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(pairWalletResponse.code).isEqualTo(testContext.pairWalletCode)
            assertThat(pairWalletResponse.publicKey).isEqualTo(testContext.publicKey)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOwnWallet() {
        suppose("User wallet exists") {
            testContext.wallet = createWalletForUser(userUuid, testContext.hash, providerId = testContext.providerId)
        }
        suppose("User has some funds on wallet") {
            testContext.balance = 100_00
            Mockito.`when`(blockchainService.getBalance(testContext.hash)).thenReturn(testContext.balance)
        }

        verify("User can get his wallet") {
            val result = mockMvc.perform(
                get(walletPath)
            )
                .andExpect(status().isOk)
                .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.uuid).isEqualTo(testContext.wallet.uuid)
            assertThat(walletResponse.hash).isEqualTo(testContext.hash)
            assertThat(walletResponse.currency).isEqualTo(testContext.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testContext.wallet.type)
            assertThat(walletResponse.providerId).isEqualTo(testContext.providerId)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(walletResponse.coop).isEqualTo(COOP)
            assertThat(walletResponse.balance).isEqualTo(testContext.balance)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForMissingWallet() {
        verify("Controller returns not found if user does not have a wallet") {
            mockMvc.perform(get(walletPath))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateWallet() {
        verify("User can create a wallet") {
            val request = WalletCreateRequest(testContext.publicKey, testContext.email, testContext.providerId)
            val result = mockMvc.perform(
                post(walletPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.uuid).isNotNull()
            assertThat(walletResponse.activationData).isEqualTo(testContext.publicKey)
            assertThat(walletResponse.email).isEqualTo(testContext.email)
            assertThat(walletResponse.providerId).isEqualTo(testContext.providerId)
            assertThat(walletResponse.currency).isEqualTo(Currency.EUR)
            assertThat(walletResponse.type).isEqualTo(WalletType.USER)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(walletResponse.hash).isNull()
            assertThat(walletResponse.activatedAt).isNull()
            assertThat(walletResponse.coop).isEqualTo(COOP)

            testContext.walletUuid = walletResponse.uuid
        }
        verify("Wallet is created") {
            val userWallet = walletRepository.findByOwner(userUuid)
            assertThat(userWallet).isPresent
            val wallet = userWallet.get()
            assertThat(wallet.activationData).isEqualTo(testContext.publicKey)
            assertThat(wallet.email).isEqualTo(testContext.email)
            assertThat(wallet.providerId).isEqualTo(testContext.providerId)
            assertThat(wallet.hash).isNull()
            assertThat(wallet.coop).isEqualTo(COOP)
        }
        verify("Mail notification for created wallet") {
            Mockito.verify(mailService, Mockito.times(1)).sendNewWalletMail(WalletTypeRequest.Type.USER)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateAdditionalWallet() {
        suppose("User wallet exists") {
            testContext.wallet = createWalletForUser(userUuid, testContext.publicKey)
        }

        verify("User cannot create additional wallet") {
            val request = WalletCreateRequest(testContext.publicKey, testContext.email, testContext.providerId)
            mockMvc.perform(
                post(walletPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfoundUser(verified = false)
    fun mustNotBeAbleToCreateWalletWithUnVerifiedAccount() {
        verify("Unverified user cannot create a wallet") {
            val request = WalletCreateRequest(testContext.publicKey, testContext.email, testContext.providerId)
            val result = mockMvc.perform(
                post(walletPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isConflict)
                .andReturn()
            val errorMessage = result.response.errorMessage
            assertThat(errorMessage).contains("User profile not verified.")
        }
    }

    /* Project Wallet */
    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetCreateProjectWalletTransaction() {
        suppose("User has a wallet") {
            testContext.wallet = createWalletForUser(userUuid, testContext.hash)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(organizationUuid, testContext.hash2)
        }
        suppose("Blockchain service successfully generates transaction to create project wallet") {
            val orgWalletHash = getWalletHash(organizationUuid)
            val userWalletHash = getWalletHash(userUuid)
            testContext.transactionData = TransactionData(signedTransaction)
            testContext.time = ZonedDateTime.now().plusDays(30)
            val request = GenerateProjectWalletRequest(
                userWalletHash, orgWalletHash, 100000, 100, 10000000,
                testContext.time.toInstant().toEpochMilli()
            )
            Mockito.`when`(
                blockchainService.generateProjectWalletTransaction(request)
            ).thenReturn(testContext.transactionData)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                projectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid, endDate = testContext.time))
        }

        verify("User can get transaction to create project wallet") {
            val result = mockMvc.perform(
                get("$projectWalletPath/$projectUuid/transaction")
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.CREATE_PROJECT)
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToGetCreateProjectWalletTransactionIfWalletExits() {
        suppose("Project wallet exists") {
            testContext.wallet = createWalletForProject(projectUuid, testContext.hash)
        }

        verify("User cannot get create project wallet transaction for additional wallet") {
            val response = mockMvc.perform(
                get("$projectWalletPath/$projectUuid/transaction")
            )
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
            testContext.wallet = createWalletForOrganization(organizationUuid, testContext.hash)
        }

        verify("User can fetch organization wallet") {
            val result = mockMvc.perform(
                get("$organizationWalletPath/$organizationUuid")
            )
                .andExpect(status().isOk)
                .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.uuid).isEqualTo(testContext.wallet.uuid)
            assertThat(walletResponse.hash).isEqualTo(testContext.hash)
            assertThat(walletResponse.currency).isEqualTo(testContext.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testContext.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(walletResponse.coop).isEqualTo(COOP)
            assertThat(walletResponse.providerId).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForMissingOrganizationWallet() {
        verify("Controller will return not found for non exiting organization wallet") {
            mockMvc.perform(get("$organizationWalletPath/${UUID.randomUUID()}"))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetCreateOrganizationWallet() {
        suppose("User wallet exists") {
            testContext.wallet = createWalletForUser(userUuid, testContext.publicKey)
        }
        suppose("Blockchain service successfully creates organization") {
            testContext.transactionData = TransactionData(signedTransaction)
            Mockito.`when`(
                blockchainService.generateCreateOrganizationTransaction(getWalletHash(userUuid))
            ).thenReturn(testContext.transactionData)
        }
        suppose("Project service will return organization") {
            Mockito.`when`(
                projectService.getOrganization(organizationUuid)
            ).thenReturn(getOrganizationResponse(organizationUuid, userUuid))
        }

        verify("User can get transaction to create organization wallet") {
            val path = "$organizationWalletPath/$organizationUuid/transaction"
            val result = mockMvc.perform(
                get(path)
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.CREATE_ORG)
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
    }

    private class TestContext {
        lateinit var wallet: Wallet
        lateinit var transactionData: TransactionData
        lateinit var walletUuid: UUID
        var hash = "th_foKr5RbgAVq84nZaF6bNfPSnjmFQ39VhQeWPetgGDwv1BNAnV"
        var hash2 = "th_2YjFd1mPzriyKfzojwuZxKJZaqNJGmTnUvqnNfwoZTV6n7NYxB"
        val publicKey = "ak_RYkcTuYcyxQ6fWZsL2G3Kj3K5WCRUEXsi76bPUNkEsoHc52Wp"
        val email = "wallet_email_is_optional"
        val providerId = "provider_id_is_optional"
        var balance: Long = -1
        lateinit var pairWalletCode: String
        lateinit var time: ZonedDateTime
    }
}
