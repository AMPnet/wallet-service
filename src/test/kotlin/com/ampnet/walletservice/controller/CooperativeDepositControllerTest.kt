package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserListResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.UsersWithApprovedDeposit
import com.ampnet.walletservice.enums.PrivilegeType
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class CooperativeDepositControllerTest : ControllerTestBase() {

    private val depositPath = "/cooperative/deposit"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
        testContext = TestContext()
    }
    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToSearchByReference() {
        suppose("User deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can search deposit by reference") {
            val savedDeposit = testContext.deposits.first()
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("$depositPath/search").param("reference", savedDeposit.reference))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val deposit: DepositWithUserResponse = objectMapper.readValue(result.response.contentAsString)
            Assertions.assertThat(deposit.reference).isEqualTo(savedDeposit.reference)
            Assertions.assertThat(deposit.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustNotBeAbleToFindByNonExistingReference() {
        suppose("User deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("Cooperative gets not found for searching deposit by non-existing reference") {
            mockMvc.perform(
                MockMvcRequestBuilders.get("$depositPath/search").param("reference", "non-existing"))
                .andExpect(MockMvcResultMatchers.status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToDeleteDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("Cooperative can delete unapproved user deposit") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("$depositPath/${testContext.deposits.first().id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
        verify("Unapproved deposit is deleted") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            Assertions.assertThat(optionalDeposit).isNotPresent
        }
        verify("Mail notification for deleting deposit is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendDepositInfo(userUuid, false)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToDeleteWithoutAdminPrivileges() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("User without admin role cannot delete unapproved deposit") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("$depositPath/${testContext.deposits.first().id}")
            )
                .andExpect(MockMvcResultMatchers.status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToApproveDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile(
                "file", "test.txt", "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("Cooperative can approve user deposit") {
            val depositId = testContext.deposits.first().id
            val result = mockMvc.perform(
                RestDocumentationRequestBuilders.fileUpload("$depositPath/$depositId/approve?amount=${testContext.amount}")
                    .file(testContext.multipartFile))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val depositResponse: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            Assertions.assertThat(depositResponse.id).isEqualTo(depositId)
            Assertions.assertThat(depositResponse.approved).isEqualTo(true)
            Assertions.assertThat(depositResponse.documentResponse?.link).isEqualTo(testContext.documentLink)
        }
        verify("User deposit is approved") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            Assertions.assertThat(optionalDeposit).isPresent
            val approvedDeposit = optionalDeposit.get()
            Assertions.assertThat(approvedDeposit.approved).isTrue()
            Assertions.assertThat(approvedDeposit.amount).isEqualTo(testContext.amount)
            Assertions.assertThat(approvedDeposit.approvedByUserUuid).isEqualTo(userUuid)
            Assertions.assertThat(approvedDeposit.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            Assertions.assertThat(approvedDeposit.file?.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustNotBeAbleToApproveNonExistingDeposit() {
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("Cooperative cannot approve non-existing deposit") {
            val result = mockMvc.perform(
                RestDocumentationRequestBuilders.fileUpload("$depositPath/0/approve")
                    .file(testContext.multipartFile)
                    .param("amount", testContext.amount.toString()))
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.WALLET_DEPOSIT_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetUnapprovedDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnapprovedDeposit(userUuid)
            val approved = createApprovedDeposit(userUuid)
            testContext.deposits = listOf(unapproved, approved)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get unapproved user deposits") {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("$depositPath/unapproved")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val deposits: DepositWithUserListResponse = objectMapper.readValue(result.response.contentAsString)
            Assertions.assertThat(deposits.deposits).hasSize(1)
            val deposit = deposits.deposits[0]
            Assertions.assertThat(deposit.approved).isFalse()
            Assertions.assertThat(deposit.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetApprovedDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnapprovedDeposit(userUuid)
            val approved = createApprovedDeposit(userUuid)
            testContext.deposits = listOf(unapproved, approved)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get approved deposits") {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("$depositPath/approved")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val deposits: DepositWithUserListResponse = objectMapper.readValue(result.response.contentAsString)
            Assertions.assertThat(deposits.deposits).hasSize(1)
            val deposit = deposits.deposits[0]
            Assertions.assertThat(deposit.approved).isTrue()
            Assertions.assertThat(deposit.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToGenerateMintTransaction() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("Approved user deposit exists") {
            val approved = createApprovedDeposit(userUuid, amount = testContext.amount)
            testContext.deposits = listOf(approved)
        }
        suppose("Blockchain service will return mint transaction") {
            testContext.transactionData = TransactionData("signed-transaction")
            Mockito.`when`(
                blockchainService.generateMintTransaction(walletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("Cooperative can generate mint transaction") {
            val depositId = testContext.deposits.first().id
            val result = mockMvc.perform(
                MockMvcRequestBuilders.post("$depositPath/$depositId/transaction")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            Assertions.assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            Assertions.assertThat(transactionResponse.txId).isNotNull()
            Assertions.assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.MINT)
        }
        verify("TransactionInfo for mint transaction is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            Assertions.assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos[0]
            Assertions.assertThat(transactionInfo.companionData).isEqualTo(testContext.deposits.first().id.toString())
            Assertions.assertThat(transactionInfo.type).isEqualTo(TransactionType.MINT)
            Assertions.assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToCountUsersWithApprovedDeposit() {
        suppose("There is unapproved deposit") {
            createUnapprovedDeposit(UUID.randomUUID())
        }
        suppose("There is approved deposit") {
            createApprovedDeposit(UUID.randomUUID())
        }
        suppose("There are approved deposits for the same user") {
            val user = UUID.randomUUID()
            createApprovedDeposit(user)
            createApprovedDeposit(user)
            createApprovedDeposit(user)
        }

        verify("Cooperative user can get statistics about counted users with approved deposit") {
            val result = mockMvc.perform(MockMvcRequestBuilders.get("$depositPath/count"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val counted: UsersWithApprovedDeposit = objectMapper.readValue(result.response.contentAsString)
            Assertions.assertThat(counted.usersWithApprovedDeposit).isEqualTo(2)
        }
    }

    private class TestContext {
        val amount = 30_000L
        var deposits = listOf<Deposit>()
        val documentLink = "document-link"
        lateinit var multipartFile: MockMultipartFile
        lateinit var transactionData: TransactionData
    }
}
