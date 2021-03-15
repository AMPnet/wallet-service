package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.DepositListResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
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

class DepositControllerTest : ControllerTestBase() {

    private val depositPath = "/deposit"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateDeposit() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User has approved deposit") {
            createApprovedDeposit(userUuid)
        }

        verify("User can create new deposit") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                post(depositPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposit: DepositServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(userUuid)
            assertThat(deposit.amount).isEqualTo(testContext.amount)
            assertThat(deposit.txHash).isNull()
            assertThat(deposit.createdBy).isEqualTo(userUuid)
            assertThat(deposit.type).isEqualTo(DepositWithdrawType.USER)
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(deposit.coop).isEqualTo(COOP)
            assertThat(deposit.documentResponse).isNull()
        }
        verify("User deposit is stored") {
            val deposits = depositRepository.findAllWithFile(COOP)
            assertThat(deposits).hasSize(2)
            val deposit = deposits.first { it.txHash == null }
            assertThat(deposit.ownerUuid).isEqualTo(userUuid)
            assertThat(deposit.txHash).isNull()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(deposit.coop).isEqualTo(COOP)
            assertThat(deposit.file).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateDepositWithoutWallet() {
        suppose("User does not has a wallet") {
            databaseCleanerService.deleteAllWallets()
        }

        verify("User cannot create deposit without wallet") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                post(depositPath)
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
    fun mustBeAbleToDeleteDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("Cooperative can delete unapproved user deposit") {
            mockMvc.perform(delete("$depositPath/${testContext.deposits.first().id}"))
                .andExpect(status().isOk)
        }
        verify("Unapproved deposit is deleted") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            assertThat(optionalDeposit).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "98986187-c870-4339-be4e-a597146f1428")
    fun mustNotBeAbleToDeleteOthersDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("User cannot delete others deposit") {
            mockMvc.perform(delete("$depositPath/${testContext.deposits.first().id}"))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetPendingDeposit() {
        suppose("User deposit exists") {
            val deposit = createUnsignedDeposit(userUuid, withFile = true)
            testContext.deposits = listOf(deposit)
        }

        verify("User can get his pending deposit") {
            val result = mockMvc.perform(get("$depositPath/pending"))
                .andExpect(status().isOk)
                .andReturn()

            val deposit: DepositServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(userUuid)
            assertThat(deposit.coop).isEqualTo(COOP)
            assertThat(deposit.documentResponse).isNotNull
            val savedDeposit = testContext.deposits.first()
            assertThat(deposit.id).isEqualTo(savedDeposit.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetDepositForTxHash() {
        suppose("User deposit exists") {
            val deposit = createApprovedDeposit(userUuid, txHash = txHash)
            testContext.deposits = listOf(deposit)
        }

        verify("User can get his deposit by txHash") {
            val result = mockMvc.perform(get(depositPath).param("txHash", txHash))
                .andExpect(status().isOk)
                .andReturn()

            val listResponse: DepositListResponse = objectMapper.readValue(result.response.contentAsString)
            val deposits = listResponse.deposits
            assertThat(deposits).hasSize(1)
            val deposit = deposits.first()
            assertThat(deposit.owner).isEqualTo(userUuid)
            assertThat(deposit.coop).isEqualTo(COOP)
            assertThat(deposit.documentResponse).isNotNull
            val savedDeposit = testContext.deposits.first()
            assertThat(deposit.id).isEqualTo(savedDeposit.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetAllDepositsIfTxHashIsNull() {
        suppose("User deposits exists") {
            val approved = createApprovedDeposit(userUuid, txHash = txHash)
            val unsigned = createUnsignedDeposit(userUuid, withFile = true)
            testContext.deposits = listOf(approved, unsigned)
        }

        verify("User can get all deposits it txHash is missing") {
            val result = mockMvc.perform(get(depositPath))
                .andExpect(status().isOk)
                .andReturn()

            val listResponse: DepositListResponse = objectMapper.readValue(result.response.contentAsString)
            val deposits = listResponse.deposits
            assertThat(deposits).hasSize(2)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetNotFoundForNoPendingDeposit() {
        verify("User gets not found for non pending deposit") {
            mockMvc.perform(get("$depositPath/pending"))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetEmptyListForNoDepositFoundForTxHash() {
        verify("User gets empty list for deposit not found for txHash") {
            val result = mockMvc.perform(get(depositPath).param("txHash", "txHash"))
                .andExpect(status().isOk)
                .andReturn()
            val listResponse: DepositListResponse = objectMapper.readValue(result.response.contentAsString)
            val deposits = listResponse.deposits
            assertThat(deposits).isEmpty()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateProjectDeposit() {
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project has approved deposit") {
            createApprovedDeposit(projectUuid)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("User can create new project deposit") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                post("$depositPath/project/$projectUuid")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposit: DepositServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(projectUuid)
            assertThat(deposit.amount).isEqualTo(testContext.amount)
            assertThat(deposit.txHash).isNull()
            assertThat(deposit.createdBy).isEqualTo(userUuid)
            assertThat(deposit.type).isEqualTo(DepositWithdrawType.PROJECT)
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(deposit.coop).isEqualTo(COOP)
            assertThat(deposit.documentResponse).isNull()
        }
        verify("Project deposit is stored") {
            val deposits = depositRepository.findAllWithFile(COOP)
            assertThat(deposits).hasSize(2)
            val deposit = deposits.first { it.txHash == null }
            assertThat(deposit.ownerUuid).isEqualTo(projectUuid)
            assertThat(deposit.txHash).isNull()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(deposit.coop).isEqualTo(COOP)
            assertThat(deposit.file).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetPendingProjectDeposit() {
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project has pending deposit") {
            val deposit = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT, withFile = true)
            testContext.deposits = listOf(deposit)
        }

        verify("User can get pending project deposit") {
            val result = mockMvc.perform(get("$depositPath/project/$projectUuid/pending"))
                .andExpect(status().isOk)
                .andReturn()
            val deposit: DepositServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(projectUuid)
            assertThat(deposit.createdBy).isEqualTo(userUuid)
            assertThat(deposit.documentResponse).isNotNull
            val savedDeposit = testContext.deposits.first()
            assertThat(deposit.id).isEqualTo(savedDeposit.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "98986187-c870-4339-be4e-a597146f1428")
    fun mustNotBeAbleToGetOtherProjectDeposit() {
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project has pending deposit") {
            val deposit = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.deposits = listOf(deposit)
        }

        verify("User can get pending project deposit") {
            val result = mockMvc.perform(get("$depositPath/project/$projectUuid/pending"))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.USER_MISSING_PRIVILEGE)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToConfirmUserDeposit() {
        suppose("There is user deposit") {
            databaseCleanerService.deleteAllDeposits()
            testContext.deposit = createApprovedDeposit(userUuid)
        }

        verify("User can confirm deposit") {
            val result = mockMvc.perform(post("$depositPath/${testContext.deposit.id}/confirm"))
                .andExpect(status().isOk)
                .andReturn()
            val deposit: DepositServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.id).isEqualTo(testContext.deposit.id)
            assertThat(deposit.userConfirmation).isEqualTo(true)
        }
        verify("Deposit is stored in the database") {
            val deposit = depositRepository.findById(testContext.deposit.id).get()
            assertThat(deposit.id).isEqualTo(testContext.deposit.id)
            assertThat(deposit.userConfirmation).isEqualTo(true)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToConfirmProjectDeposit() {
        suppose("There is a project deposit") {
            databaseCleanerService.deleteAllDeposits()
            testContext.deposit = createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
        }

        verify("User can confirm project deposit") {
            val result = mockMvc.perform(post("$depositPath/${testContext.deposit.id}/confirm"))
                .andExpect(status().isOk)
                .andReturn()
            val deposit: DepositServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.id).isEqualTo(testContext.deposit.id)
            assertThat(deposit.userConfirmation).isEqualTo(true)
        }
        verify("Deposit is stored in the database") {
            val deposit = depositRepository.findById(testContext.deposit.id).get()
            assertThat(deposit.id).isEqualTo(testContext.deposit.id)
            assertThat(deposit.userConfirmation).isEqualTo(true)
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "98986187-c870-4339-be4e-a597146f1428")
    fun mustNotBeAbleToConfirmDepositIfNotCreatedByUser() {
        suppose("There is a user deposit") {
            databaseCleanerService.deleteAllDeposits()
            testContext.deposit = createApprovedDeposit(userUuid)
        }

        verify("User cannot confirm deposit he did not create") {
            val result = mockMvc.perform(post("$depositPath/${testContext.deposit.id}/confirm"))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.WALLET_DEPOSIT_MISSING)
        }
    }

    private class TestContext {
        val amount = 30_000L
        var deposits = listOf<Deposit>()
        lateinit var deposit: Deposit
    }
}
