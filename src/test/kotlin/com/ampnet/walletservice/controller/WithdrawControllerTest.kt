package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawResponse
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class WithdrawControllerTest : ControllerTestBase() {

    private val withdrawPath = "/withdraw"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateWithdraw() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User has enough funds on wallet") {
            Mockito.`when`(blockchainService.getBalance(walletHash))
                .thenReturn(testContext.amount)
        }

        verify("User can create Withdraw") {
            val request = WithdrawCreateRequest(testContext.amount, testContext.bankAccount)
            val result = mockMvc.perform(
                post(withdrawPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.owner).isEqualTo(userUuid)
            assertThat(withdrawResponse.amount).isEqualTo(testContext.amount)
            assertThat(withdrawResponse.bankAccount).isEqualTo(testContext.bankAccount)
            assertThat(withdrawResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdrawResponse.approvedAt).isNull()
            assertThat(withdrawResponse.approvedAt).isNull()
            assertThat(withdrawResponse.approvedTxHash).isNull()
            assertThat(withdrawResponse.burnedAt).isNull()
            assertThat(withdrawResponse.burnedTxHash).isNull()
            assertThat(withdrawResponse.burnedBy).isNull()
            assertThat(withdrawResponse.documentResponse).isNull()
        }
        verify("Withdraw is created") {
            val withdraws = withdrawRepository.findAll()
            assertThat(withdraws).hasSize(1)
            val withdraw = withdraws.first()
            assertThat(withdraw.ownerUuid).isEqualTo(userUuid)
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.bankAccount).isEqualTo(testContext.bankAccount)
            assertThat(withdraw.createdBy).isEqualTo(userUuid)
            assertThat(withdraw.approvedAt).isNull()
            assertThat(withdraw.approvedTxHash).isNull()
            assertThat(withdraw.burnedAt).isNull()
            assertThat(withdraw.burnedTxHash).isNull()
            assertThat(withdraw.burnedBy).isNull()
            assertThat(withdraw.file).isNull()
        }
        verify("Mail notification for created withdraw is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendWithdrawRequest(userUuid, testContext.amount)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateProjectWithdraw() {
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project has enough funds on wallet") {
            Mockito.`when`(blockchainService.getBalance(walletHash))
                .thenReturn(testContext.amount)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("User can create Project Withdraw") {
            val request = WithdrawCreateRequest(testContext.amount, testContext.bankAccount)
            val result = mockMvc.perform(
                post("$withdrawPath/project/$projectUuid")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.owner).isEqualTo(projectUuid)
            assertThat(withdrawResponse.amount).isEqualTo(testContext.amount)
            assertThat(withdrawResponse.bankAccount).isEqualTo(testContext.bankAccount)
            assertThat(withdrawResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdrawResponse.approvedAt).isNull()
            assertThat(withdrawResponse.approvedAt).isNull()
            assertThat(withdrawResponse.approvedTxHash).isNull()
            assertThat(withdrawResponse.burnedAt).isNull()
            assertThat(withdrawResponse.burnedTxHash).isNull()
            assertThat(withdrawResponse.burnedBy).isNull()
            assertThat(withdrawResponse.documentResponse).isNull()
        }
        verify("Withdraw is created") {
            val withdraws = withdrawRepository.findAll()
            assertThat(withdraws).hasSize(1)
            val withdraw = withdraws.first()
            assertThat(withdraw.ownerUuid).isEqualTo(projectUuid)
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.bankAccount).isEqualTo(testContext.bankAccount)
            assertThat(withdraw.createdBy).isEqualTo(userUuid)
            assertThat(withdraw.approvedAt).isNull()
            assertThat(withdraw.approvedTxHash).isNull()
            assertThat(withdraw.burnedAt).isNull()
            assertThat(withdraw.burnedTxHash).isNull()
            assertThat(withdraw.burnedBy).isNull()
            assertThat(withdraw.file).isNull()
        }
        verify("Mail notification for created project withdraw to user is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendWithdrawRequest(userUuid, testContext.amount)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateWithdrawWithoutUserWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWallets()
        }

        verify("Controller will return bad request if user is missing wallet") {
            val request = WithdrawCreateRequest(testContext.amount, testContext.bankAccount)
            val result = mockMvc.perform(
                post(withdrawPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetPendingWithdraw() {
        suppose("User has created withdraw") {
            testContext.withdraw = createWithdraw(userUuid)
        }

        verify("User can get his pending withdraw") {
            val result = mockMvc.perform(get(withdrawPath))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.owner).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetProjectPendingWithdraw() {
        suppose("Project has created withdraw") {
            testContext.withdraw = createWithdraw(projectUuid, type = WalletType.PROJECT)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("User can get pending project withdraw") {
            val result = mockMvc.perform(get("$withdrawPath/project/$projectUuid"))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.owner).isEqualTo(projectUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeGetNotFoundForNoPendingWithdraw() {
        verify("User will get not found for no pending withdraw") {
            mockMvc.perform(get(withdrawPath))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToDeleteWithdraw() {
        suppose("Approved withdraw is created") {
            testContext.withdraw = createApprovedWithdraw(userUuid)
        }

        verify("User can delete withdraw") {
            mockMvc.perform(delete("$withdrawPath/${testContext.withdraw.id}"))
                .andExpect(status().isOk)
        }
        verify("Withdraw is deleted") {
            val deletedWithdraw = withdrawRepository.findById(testContext.withdraw.id)
            assertThat(deletedWithdraw).isNotPresent
        }
        verify("Mail notification for deleted withdraw is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendWithdrawInfo(userUuid, false)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGenerateApprovalTransaction() {
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User has created withdraw") {
            testContext.withdraw = createWithdraw(userUuid)
        }
        suppose("Blockchain service will return approve burn transaction") {
            testContext.transactionData = TransactionData("approve-burn-transaction")
            Mockito.`when`(
                    blockchainService.generateApproveBurnTransaction(walletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate approval transaction") {
            val result = mockMvc.perform(
                post("$withdrawPath/${testContext.withdraw.id}/transaction/approve"))
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.BURN_APPROVAL)
        }
        verify("Transaction info is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos.first()
            assertThat(transactionInfo.companionData).isEqualTo(testContext.withdraw.id.toString())
            assertThat(transactionInfo.type).isEqualTo(TransactionType.BURN_APPROVAL)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGenerateApprovalTransactionForProject() {
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("Project has wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project has withdraw") {
            testContext.withdraw = createWithdraw(projectUuid, type = WalletType.PROJECT, userUuid = userUuid)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }
        suppose("Blockchain service will return approve burn transaction") {
            testContext.transactionData = TransactionData("approve-burn-transaction")
            Mockito.`when`(
                blockchainService.generateApproveBurnTransaction(walletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate approval transaction") {
            val result = mockMvc.perform(
                post("$withdrawPath/${testContext.withdraw.id}/transaction/approve"))
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.BURN_APPROVAL)
        }
        verify("Transaction info is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos.first()
            assertThat(transactionInfo.companionData).isEqualTo(testContext.withdraw.id.toString())
            assertThat(transactionInfo.type).isEqualTo(TransactionType.BURN_APPROVAL)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    private class TestContext {
        val amount = 1000L
        val bankAccount = "AL35202111090000000001234567"
        lateinit var withdraw: Withdraw
        lateinit var transactionData: TransactionData
    }
}
