package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.CommentRequest
import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithProjectListResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserListResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.UsersWithApprovedDeposit
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.enums.PrivilegeType
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

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
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can search deposit by reference") {
            val savedDeposit = testContext.deposits.first()
            val result = mockMvc.perform(
                get("$depositPath/search").param("reference", savedDeposit.reference)
            )
                .andExpect(status().isOk)
                .andReturn()

            val response: DepositWithUserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.deposit.reference).isEqualTo(savedDeposit.reference)
            assertThat(response.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustNotBeAbleToFindByNonExistingReference() {
        suppose("User deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("Cooperative gets not found for searching deposit by non-existing reference") {
            mockMvc.perform(
                get("$depositPath/search").param("reference", "non-existing")
            )
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToDeclineDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("Cooperative can decline user deposit") {
            val request = CommentRequest("Decline!")
            val depositId = testContext.deposits.first().id
            val result = mockMvc.perform(
                post("$depositPath/$depositId/decline")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val depositResponse: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(depositResponse.id).isEqualTo(depositId)
            assertThat(depositResponse.txHash).isNull()
            assertThat(depositResponse.declinedComment).isEqualTo(request.comment)
            assertThat(depositResponse.declinedAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("User deposit is declined") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            assertThat(optionalDeposit).isPresent
            val declinedDeposit = optionalDeposit.get()
            assertThat(declinedDeposit.txHash).isNull()
            assertThat(declinedDeposit.approvedByUserUuid).isNull()
            assertThat(declinedDeposit.approvedAt).isNull()
            assertThat(declinedDeposit.file?.link).isNull()
            assertThat(declinedDeposit.declined?.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(declinedDeposit.declined?.createdBy).isEqualTo(userUuid)
            assertThat(declinedDeposit.declined?.comment).isNotNull()
        }
        verify("Mail notification for declining deposit is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendDepositInfo(userUuid, false)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToApproveDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile(
                "file", "test.txt", "text/plain", "Some document data".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes
                )
            ).thenReturn(testContext.documentLink)
        }

        verify("Cooperative can approve user deposit") {
            val depositId = testContext.deposits.first().id
            val result = mockMvc.perform(
                RestDocumentationRequestBuilders.fileUpload("$depositPath/$depositId/approve?amount=${testContext.amount}")
                    .file(testContext.multipartFile)
            )
                .andExpect(status().isOk)
                .andReturn()

            val depositResponse: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(depositResponse.id).isEqualTo(depositId)
            assertThat(depositResponse.approvedAt).isNotNull()
            assertThat(depositResponse.documentResponse?.link).isEqualTo(testContext.documentLink)
            assertThat(depositResponse.declinedComment).isNull()
            assertThat(depositResponse.declinedAt).isNull()
        }
        verify("User deposit is approved") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            assertThat(optionalDeposit).isPresent
            val approvedDeposit = optionalDeposit.get()
            assertThat(approvedDeposit.amount).isEqualTo(testContext.amount)
            assertThat(approvedDeposit.approvedByUserUuid).isEqualTo(userUuid)
            assertThat(approvedDeposit.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(approvedDeposit.file?.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustNotBeAbleToApproveNonExistingDeposit() {
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile(
                "file", "test.txt",
                "text/plain", "Some document data".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes
                )
            ).thenReturn(testContext.documentLink)
        }

        verify("Cooperative cannot approve non-existing deposit") {
            val result = mockMvc.perform(
                RestDocumentationRequestBuilders.fileUpload("$depositPath/0/approve")
                    .file(testContext.multipartFile)
                    .param("amount", testContext.amount.toString())
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.WALLET_DEPOSIT_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetUnapprovedDeposits() {
        suppose("Approved, unsigned and signed deposits exist") {
            val unapproved = createUnsignedDeposit(userUuid)
            val unsigned = createUnsignedDeposit(userUuid, withFile = true)
            createApprovedDeposit(userUuid)
            testContext.deposits = listOf(unapproved, unsigned)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get unapproved user deposits") {
            val result = mockMvc.perform(
                get("$depositPath/unapproved")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositWithUserListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(2)
            val firstDeposit = deposits.deposits[0]
            assertThat(firstDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(firstDeposit.deposit.txHash).isNull()
            assertThat(firstDeposit.user).isNotNull
            val secondDeposit = deposits.deposits[1]
            assertThat(secondDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(secondDeposit.deposit.txHash).isNull()
            assertThat(secondDeposit.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetUnapprovedProjectDeposits() {
        suppose("Approved, unsigned and signed project deposits exist") {
            val unapproved = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            val unsigned = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT, withFile = true)
            createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.deposits = listOf(unapproved, unsigned)
        }
        suppose("User service will return user") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }

        verify("Cooperative can get unapproved user deposits") {
            val result = mockMvc.perform(
                get("$depositPath/unapproved/project")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositWithProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(2)
            val firstDeposit = deposits.deposits[0]
            assertThat(firstDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(firstDeposit.deposit.txHash).isNull()
            assertThat(firstDeposit.project).isNotNull
            val secondDeposit = deposits.deposits[1]
            assertThat(secondDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(secondDeposit.deposit.txHash).isNull()
            assertThat(secondDeposit.project).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetApprovedDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnsignedDeposit(userUuid)
            val approved = createApprovedDeposit(userUuid)
            testContext.deposits = listOf(unapproved, approved)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get approved deposits") {
            val result = mockMvc.perform(
                get("$depositPath/approved")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositWithUserListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(1)
            val response = deposits.deposits[0]
            assertThat(response.deposit.txHash).isNotNull()
            assertThat(response.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetApprovedProjectDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            val approved = createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.deposits = listOf(unapproved, approved)
        }
        suppose("Project service will return project data") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get approved project deposits") {
            val result = mockMvc.perform(
                get("$depositPath/approved/project")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositWithProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(1)
            val response = deposits.deposits[0]
            assertThat(response.deposit.txHash).isNotNull()
            assertThat(response.project?.uuid).isEqualTo(projectUuid.toString())
            assertThat(response.user?.uuid).isEqualTo(userUuid)
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
            val approved = createUnsignedDeposit(userUuid, withFile = true, amount = testContext.amount)
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
                post("$depositPath/$depositId/transaction")
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.MINT)
        }
        verify("TransactionInfo for mint transaction is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos[0]
            assertThat(transactionInfo.companionData).isEqualTo(testContext.deposits.first().id.toString())
            assertThat(transactionInfo.type).isEqualTo(TransactionType.MINT)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToCountUsersWithApprovedDeposit() {
        suppose("There is unapproved deposit") {
            createUnsignedDeposit(UUID.randomUUID())
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
            val result = mockMvc.perform(get("$depositPath/count"))
                .andExpect(status().isOk)
                .andReturn()

            val counted: UsersWithApprovedDeposit = objectMapper.readValue(result.response.contentAsString)
            assertThat(counted.usersWithApprovedDeposit).isEqualTo(2)
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
