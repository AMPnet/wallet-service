package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawListResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.grpc.blockchain.pojo.ApproveProjectBurnTransactionRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

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
            val request = WithdrawCreateRequest(testContext.amount, testContext.bankAccount, testContext.bankCode)
            val result = mockMvc.perform(
                post(withdrawPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawServiceResponse = objectMapper.readValue(result.response.contentAsString)
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
            assertThat(withdrawResponse.coop).isEqualTo(COOP)
            assertThat(withdrawResponse.bankCode).isEqualTo(testContext.bankCode)
        }
        verify("Withdraw is created") {
            val withdraws = withdrawRepository.findAllWithFile(COOP)
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
            assertThat(withdraw.coop).isEqualTo(COOP)
            assertThat(withdraw.bankCode).isEqualTo(testContext.bankCode)
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
            val request = WithdrawCreateRequest(testContext.amount, testContext.bankAccount, testContext.bankCode)
            val result = mockMvc.perform(
                post("$withdrawPath/project/$projectUuid")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawServiceResponse = objectMapper.readValue(result.response.contentAsString)
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
            assertThat(withdrawResponse.coop).isEqualTo(COOP)
            assertThat(withdrawResponse.bankCode).isEqualTo(testContext.bankCode)
        }
        verify("Withdraw is created") {
            val withdraws = withdrawRepository.findAllWithFile(COOP)
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
            assertThat(withdraw.coop).isEqualTo(COOP)
            assertThat(withdraw.bankCode).isEqualTo(testContext.bankCode)
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
            val request = WithdrawCreateRequest(testContext.amount, testContext.bankAccount, null)
            val result = mockMvc.perform(
                post(withdrawPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
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
            val result = mockMvc.perform(get("$withdrawPath/pending"))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.owner).isEqualTo(userUuid)
            assertThat(withdrawResponse.coop).isEqualTo(COOP)
            assertThat(withdrawResponse.documentResponse).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetProjectPendingWithdraw() {
        suppose("Project has created withdraw") {
            testContext.withdraw = createWithdraw(projectUuid, type = DepositWithdrawType.PROJECT)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("User can get pending project withdraw") {
            val result = mockMvc.perform(get("$withdrawPath/project/$projectUuid/pending"))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.owner).isEqualTo(projectUuid)
            assertThat(withdrawResponse.coop).isEqualTo(COOP)
            assertThat(withdrawResponse.documentResponse).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetWithdrawForTxHash() {
        suppose("User has approved withdraw") {
            testContext.withdraw = createApprovedWithdraw(userUuid, txHash = txHash, withFile = true)
        }
        suppose("User has another withdraw") {
            createApprovedWithdraw(userUuid, withFile = true)
        }

        verify("User can get withdraw by tx hash") {
            val result = mockMvc.perform(get(withdrawPath).param("txHash", txHash))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawListResponse = objectMapper.readValue(result.response.contentAsString)
            val withdraw = withdrawResponse.withdraws.first()
            assertThat(withdraw.owner).isEqualTo(userUuid)
            assertThat(withdraw.coop).isEqualTo(COOP)
            assertThat(withdraw.documentResponse).isNotNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetAllWithdrawsIfTxHashIsNull() {
        suppose("User has approved and unsigned withdraws") {
            val approved = createApprovedWithdraw(userUuid)
            val unsigned = createWithdraw(userUuid)
            testContext.withdraws = listOf(approved, unsigned)
        }

        verify("User can get all withdraws it txHash is missing") {
            val result = mockMvc.perform(get(withdrawPath))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawListResponse = objectMapper.readValue(result.response.contentAsString)
            val withdraws = withdrawResponse.withdraws
            assertThat(withdraws).hasSize(2)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetNotFoundForNoPendingWithdraw() {
        verify("User will get not found for no pending withdraw") {
            mockMvc.perform(get("$withdrawPath/pending"))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetEmptyListForNoWithdrawFoundForTxHash() {
        verify("User gets empty list for deposit not found for txHash") {
            val result = mockMvc.perform(get(withdrawPath))
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawListResponse = objectMapper.readValue(result.response.contentAsString)
            val withdraws = withdrawResponse.withdraws
            assertThat(withdraws).isEmpty()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToDeleteWithdraw() {
        suppose("Approved withdraw is created") {
            testContext.withdraw = createWithdraw(userUuid)
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
                post("$withdrawPath/${testContext.withdraw.id}/transaction/approve")
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.BURN_APPROVAL)
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
        verify("Transaction info is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos.first()
            assertThat(transactionInfo.companionData).isEqualTo(testContext.withdraw.id.toString())
            assertThat(transactionInfo.type).isEqualTo(TransactionType.BURN_APPROVAL)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
            assertThat(transactionInfo.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGenerateApprovalTransactionForProject() {
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("User and project have wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
            createWalletForProject(projectUuid, testContext.projectWalletHash)
        }
        suppose("Project has withdraw") {
            testContext.withdraw = createWithdraw(projectUuid, type = DepositWithdrawType.PROJECT, userUuid = userUuid)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }
        suppose("Blockchain service will return approve burn transaction") {
            testContext.transactionData = TransactionData("approve-burn-transaction")
            val request = ApproveProjectBurnTransactionRequest(
                testContext.projectWalletHash, testContext.amount, walletHash
            )
            Mockito.`when`(
                blockchainService.generateApproveProjectBurnTransaction(request)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate approval transaction") {
            val result = mockMvc.perform(
                post("$withdrawPath/${testContext.withdraw.id}/transaction/approve")
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.BURN_APPROVAL)
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
        verify("Transaction info is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos.first()
            assertThat(transactionInfo.companionData).isEqualTo(testContext.withdraw.id.toString())
            assertThat(transactionInfo.type).isEqualTo(TransactionType.BURN_APPROVAL)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
            assertThat(transactionInfo.coop).isEqualTo(COOP)
        }
    }

    private class TestContext {
        val amount = 1000L
        val bankAccount = "AL35202111090000000001234567"
        val projectWalletHash = "th_foKr5RbgAVq84nZaF6bNfPSnjmFQ39VhQeWPetgGDwv1BNAnV"
        val bankCode = "BACXROBU"
        lateinit var withdraws: List<Withdraw>
        lateinit var withdraw: Withdraw
        lateinit var transactionData: TransactionData
    }
}
