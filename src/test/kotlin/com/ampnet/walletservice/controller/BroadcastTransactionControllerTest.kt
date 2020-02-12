package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.TxBroadcastRequest
import com.ampnet.walletservice.controller.pojo.response.TxHashResponse
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.model.Withdraw
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BroadcastTransactionControllerTest : ControllerTestBase() {

    private val broadcastPath = "/tx_broadcast"
    private val txHash = "th_2cNtX3hdmGPHq8sgHb6Lcu87iEc3E6feHTWczQAViQjmP7evbP"
    private val activationData = "activation_data"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllWallets()
        databaseCleanerService.deleteAllTransactionInfo()
    }

    @Test
    fun mustNotBeAbleToPostNonExistingTransaction() {
        verify("User cannot post signed non existing transaction") {
            val request = TxBroadcastRequest(0, signedTransaction)
            val response = mockMvc.perform(
                    post(broadcastPath)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.TX_MISSING)
        }
    }

    @Test
    fun mustBeAbleToActivateWallet() {
        suppose("TransactionInfo exists for activation wallet") {
            testContext.wallet = createUnactivatedWallet(userUuid)
            testContext.transactionInfo =
                createTransactionInfo(TransactionType.WALLET_ACTIVATE, userUuid, testContext.wallet.uuid.toString())
        }
        suppose("Blockchain service successfully generates transaction to create organization wallet") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(txHash)
        }

        verify("User can create organization wallet") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("Wallet is activated") {
            val optionalWallet = walletRepository.findById(testContext.wallet.uuid)
            assertThat(optionalWallet).isPresent
            val wallet = optionalWallet.get()
            assertThat(wallet.hash).isEqualTo(txHash)
            assertThat(wallet.activatedAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustBeAbleToCreateOrganizationWallet() {
        suppose("TransactionInfo exists for create organization wallet") {
            testContext.transactionInfo =
                createTransactionInfo(TransactionType.CREATE_ORG, userUuid, organizationUuid.toString())
        }
        suppose("Blockchain service successfully generates transaction to create organization wallet") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(activationData)
        }

        verify("User can create organization wallet") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(activationData)
        }
        verify("Organization wallet is created") {
            val optionalWallet = walletRepository.findByOwner(organizationUuid)
            assertThat(optionalWallet).isPresent
            val organizationWallet = optionalWallet.get()
            assertThat(organizationWallet.uuid).isNotNull()
            assertThat(organizationWallet.activationData).isEqualTo(activationData)
            assertThat(organizationWallet.currency).isEqualTo(Currency.EUR)
            assertThat(organizationWallet.type).isEqualTo(WalletType.ORG)
            assertThat(organizationWallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(organizationWallet.hash).isNull()
            assertThat(organizationWallet.activatedAt).isNull()
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustThrowErrorIfCompanionOrganizationIdIsMissing() {
        suppose("TransactionInfo exists for create organization wallet but without companion org id") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.CREATE_ORG, userUuid)
        }

        verify("User can create organization wallet") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.TX_COMPANION_DATA_MISSING)
        }
    }

    @Test
    fun mustBeAbleToCreateProjectWalletWithTransaction() {
        suppose("TransactionInfo exists for create project wallet") {
            testContext.transactionInfo =
                createTransactionInfo(TransactionType.CREATE_PROJECT, userUuid, projectUuid.toString())
        }
        suppose("Blockchain service successfully adds project wallet") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(activationData)
        }

        verify("User can create project wallet") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(activationData)
        }
        verify("Wallet is created") {
            val optionalWallet = walletRepository.findByOwner(projectUuid)
            assertThat(optionalWallet).isPresent
            val projectWallet = optionalWallet.get()
            assertThat(projectWallet.uuid).isNotNull()
            assertThat(projectWallet.activationData).isEqualTo(activationData)
            assertThat(projectWallet.currency).isEqualTo(Currency.EUR)
            assertThat(projectWallet.type).isEqualTo(WalletType.PROJECT)
            assertThat(projectWallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(projectWallet.hash).isNull()
            assertThat(projectWallet.activatedAt).isNull()
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustThrowErrorIfCompanionProjectIdIsMissing() {
        suppose("TransactionInfo exists for create project wallet but without companion project id") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.CREATE_PROJECT, userUuid)
        }

        verify("User can create organization wallet") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.TX_COMPANION_DATA_MISSING)
        }
    }

    @Test
    fun mustBeAbleToPostSignedInvestAllowanceTransaction() {
        suppose("TransactionInfo exists for invest allowance transaction") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.INVEST, userUuid)
        }
        suppose("Blockchain service will accept signed transaction for project investment") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(txHash)
        }

        verify("User can post signed transaction to invest in project") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustBeAbleToPostSignedInvestTransaction() {
        suppose("TransactionInfo exists for invest transaction") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.INVEST, userUuid)
        }
        suppose("Blockchain service will accept signed transaction for project investment confirmation") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(txHash)
        }

        verify("User can post signed transaction to confirm investment in project") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustBeAbleToPostSignedMintTransaction() {
        suppose("Deposit approved exists") {
            testContext.deposit = createApprovedDeposit(userUuid)
        }
        suppose("TransactionInfo exists for invest transaction") {
            testContext.transactionInfo =
                createTransactionInfo(TransactionType.MINT, userUuid, testContext.deposit.id.toString())
        }
        suppose("Blockchain service will accept signed transaction for project investment confirmation") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(txHash)
        }

        verify("User can post signed transaction to confirm investment in project") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
        verify("Mail notification is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendDepositInfo(userUuid, true)
        }
    }

    @Test
    fun mustBeAbleToPostSignedBurnApprovalTransaction() {
        suppose("Withdraw exists") {
            testContext.withdraw = createWithdraw(userUuid)
        }
        suppose("TransactionInfo exists for withdraw approval transaction") {
            testContext.transactionInfo =
                createTransactionInfo(TransactionType.BURN_APPROVAL, userUuid, testContext.withdraw.id.toString())
        }
        suppose("Blockchain service will accept signed transaction for burn approval") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(txHash)
        }

        verify("User can post signed transaction to confirm burn approval") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustBeAbleToPostSignedBurnTransaction() {
        suppose("Withdraw approved exists") {
            testContext.withdraw = createApprovedWithdraw(userUuid)
        }
        suppose("TransactionInfo exists for withdraw burn transaction") {
            testContext.transactionInfo =
                createTransactionInfo(TransactionType.BURN, userUuid, testContext.withdraw.id.toString())
        }
        suppose("Blockchain service will accept signed transaction for issuer burn") {
            Mockito.`when`(blockchainService.postTransaction(signedTransaction)).thenReturn(txHash)
        }

        verify("User can post signed transaction to confirm burn") {
            val request = TxBroadcastRequest(testContext.transactionInfo.id, signedTransaction)
            val result = mockMvc.perform(
                post(broadcastPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
        verify("Mail notification is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendWithdrawInfo(userUuid, true)
        }
    }

    private fun createTransactionInfo(
        type: TransactionType,
        userUuid: UUID,
        companionData: String? = null
    ): TransactionInfo {
        val transactionInfo = TransactionInfo(0, type, "description", userUuid, companionData)
        return transactionInfoRepository.save(transactionInfo)
    }

    private fun createUnactivatedWallet(owner: UUID): Wallet {
        val wallet = Wallet(UUID.randomUUID(), owner, "activation-data", WalletType.USER, Currency.EUR, ZonedDateTime.now(), null, null)
        return walletRepository.save(wallet)
    }

    private class TestContext {
        lateinit var transactionInfo: TransactionInfo
        lateinit var deposit: Deposit
        lateinit var withdraw: Withdraw
        lateinit var wallet: Wallet
    }
}
