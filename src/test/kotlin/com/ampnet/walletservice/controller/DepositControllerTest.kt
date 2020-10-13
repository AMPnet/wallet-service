package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
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
import java.util.UUID

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

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(userUuid)
            assertThat(deposit.amount).isEqualTo(testContext.amount)
            assertThat(deposit.txHash).isNull()
            assertThat(deposit.createdBy).isEqualTo(userUuid)
            assertThat(deposit.type).isEqualTo(DepositWithdrawType.USER)
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("User deposit is stored") {
            val deposits = depositRepository.findAll()
            assertThat(deposits).hasSize(2)
            val deposit = deposits.first { it.txHash != null }
            assertThat(deposit.ownerUuid).isEqualTo(userUuid)
            assertThat(deposit.txHash).isNotNull()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
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
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToDeleteOthersDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnsignedDeposit(UUID.randomUUID())
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
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("User can his get pending deposit") {
            val result = mockMvc.perform(get(depositPath))
                .andExpect(status().isOk)
                .andReturn()

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(userUuid)
            val savedDeposit = testContext.deposits.first()
            assertThat(deposit.id).isEqualTo(savedDeposit.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetNotFoundForNoPendingDeposit() {
        verify("User gets not found for non pending deposit") {
            mockMvc.perform(get(depositPath))
                .andExpect(status().isNotFound)
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

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(projectUuid)
            assertThat(deposit.amount).isEqualTo(testContext.amount)
            assertThat(deposit.txHash).isNull()
            assertThat(deposit.createdBy).isEqualTo(userUuid)
            assertThat(deposit.type).isEqualTo(DepositWithdrawType.PROJECT)
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Project deposit is stored") {
            val deposits = depositRepository.findAll()
            assertThat(deposits).hasSize(2)
            val deposit = deposits.first { it.txHash == null }
            assertThat(deposit.ownerUuid).isEqualTo(projectUuid)
            assertThat(deposit.txHash).isNull()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
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
            val deposit = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.deposits = listOf(deposit)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("User can get pending project deposit") {
            val result = mockMvc.perform(get("$depositPath/project/$projectUuid"))
                .andExpect(status().isOk)
                .andReturn()
            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.owner).isEqualTo(projectUuid)
            assertThat(deposit.createdBy).isEqualTo(userUuid)
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
            val result = mockMvc.perform(get("$depositPath/project/$projectUuid"))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.USER_MISSING_PRIVILEGE)
        }
    }

    private class TestContext {
        val amount = 30_000L
        var deposits = listOf<Deposit>()
    }
}
