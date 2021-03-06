package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.enums.PrivilegeType
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.ampnet.walletservice.service.pojo.response.DepositListServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositWithDataServiceResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

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
            testContext.deposits = mutableListOf(deposit)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }
        suppose("There is deposit from another coop") {
            val anotherCoopDeposit = createUnsignedDeposit(userUuid, coop = anotherCoop)
            testContext.deposits.add(anotherCoopDeposit)
        }

        verify("Cooperative can search deposit by reference") {
            val savedDeposit = testContext.deposits.first()
            val result = mockMvc.perform(
                get("$depositPath/search").param("reference", savedDeposit.reference)
            )
                .andExpect(status().isOk)
                .andReturn()

            val response: DepositWithDataServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.deposit.reference).isEqualTo(savedDeposit.reference)
            assertThat(response.user).isNotNull
            assertThat(response.deposit.coop).isEqualTo(COOP)
            assertThat(response.project).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustNotBeAbleToFindByNonExistingReference() {
        suppose("User deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = mutableListOf(deposit)
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
    fun mustBeAbleToDeleteDeposit() {
        suppose("Unapproved user deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = mutableListOf(deposit)
        }

        verify("Cooperative can delete user deposit") {
            val depositId = testContext.deposits.first().id
            mockMvc.perform(delete("$depositPath/$depositId")).andExpect(status().isOk)
        }
        verify("User deposit is deleted") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            assertThat(optionalDeposit).isNotPresent
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
            testContext.deposits = mutableListOf(deposit)
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

            val depositResponse: DepositServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(depositResponse.id).isEqualTo(depositId)
            assertThat(depositResponse.approvedAt).isNotNull()
            assertThat(depositResponse.documentResponse?.link).isEqualTo(testContext.documentLink)
            assertThat(depositResponse.coop).isEqualTo(COOP)
        }
        verify("User deposit is approved") {
            val optionalDeposit = depositRepository.findWithFileById(COOP, testContext.deposits.first().id)
            assertThat(optionalDeposit).isPresent
            val approvedDeposit = optionalDeposit.get()
            assertThat(approvedDeposit.amount).isEqualTo(testContext.amount)
            assertThat(approvedDeposit.approvedByUserUuid).isEqualTo(userUuid)
            assertThat(approvedDeposit.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(approvedDeposit.coop).isEqualTo(COOP)
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
            testContext.deposits = mutableListOf(unapproved, unsigned)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }
        suppose("There is unapproved deposit from another coop") {
            createUnsignedDeposit(userUuid, coop = anotherCoop)
        }

        verify("Cooperative can get unapproved user deposits") {
            val result = mockMvc.perform(
                get("$depositPath/unapproved")
                    .param("type", DepositWithdrawType.USER.name)
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(2)
            val firstDeposit = deposits.deposits[0]
            assertThat(firstDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(firstDeposit.deposit.txHash).isNull()
            assertThat(firstDeposit.user).isNotNull
            assertThat(firstDeposit.project).isNull()
            assertThat(firstDeposit.deposit.documentResponse).isNull()
            assertThat(firstDeposit.deposit.coop).isEqualTo(COOP)
            val secondDeposit = deposits.deposits[1]
            assertThat(secondDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(secondDeposit.deposit.txHash).isNull()
            assertThat(secondDeposit.user).isNotNull
            assertThat(secondDeposit.project).isNull()
            assertThat(secondDeposit.deposit.documentResponse).isNull()
            assertThat(secondDeposit.deposit.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetUnapprovedProjectDeposits() {
        suppose("Approved, unsigned and signed project deposits exist") {
            val unapproved = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            val unsigned = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT, withFile = true)
            createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.deposits = mutableListOf(unapproved, unsigned)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }
        suppose("There is unapproved project deposit from another coop") {
            val anotherCoopDeposit = createUnsignedDeposit(projectUuid, DepositWithdrawType.PROJECT, coop = anotherCoop)
            testContext.deposits.add(anotherCoopDeposit)
        }

        verify("Cooperative can get unapproved project deposits") {
            val result = mockMvc.perform(
                get("$depositPath/unapproved")
                    .param("type", DepositWithdrawType.PROJECT.name)
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(2)
            val firstDeposit = deposits.deposits[0]
            assertThat(firstDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(firstDeposit.deposit.txHash).isNull()
            assertThat(firstDeposit.project).isNotNull
            assertThat(firstDeposit.user).isNotNull
            assertThat(firstDeposit.deposit.documentResponse).isNull()
            assertThat(firstDeposit.deposit.coop).isEqualTo(COOP)
            val secondDeposit = deposits.deposits[1]
            assertThat(secondDeposit.deposit.id).isIn(testContext.deposits.map { it.id })
            assertThat(secondDeposit.deposit.txHash).isNull()
            assertThat(secondDeposit.project).isNotNull
            assertThat(secondDeposit.user).isNotNull
            assertThat(secondDeposit.deposit.documentResponse).isNull()
            assertThat(secondDeposit.deposit.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetAllUnapprovedDeposits() {
        suppose("Approved, unsigned and signed user and project deposits exist") {
            createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT, withFile = true)
            createUnsignedDeposit(userUuid)
            createUnsignedDeposit(userUuid, withFile = true)
            createApprovedDeposit(userUuid)
            createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }
        suppose("There is approved deposit from another coop") {
            val anotherCoopDeposit = createApprovedDeposit(userUuid, coop = anotherCoop)
            testContext.deposits.add(anotherCoopDeposit)
        }

        verify("Cooperative can get unapproved user and project deposits for unspecified type") {
            val result = mockMvc.perform(
                get("$depositPath/unapproved")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(4)
            assertThat(deposits.deposits.filter { it.deposit.coop != COOP }).hasSize(0)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetApprovedDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnsignedDeposit(userUuid)
            val approved = createApprovedDeposit(userUuid)
            testContext.deposits = mutableListOf(unapproved, approved)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }
        suppose("There is usigned deposit from another coop") {
            val anotherCoopDeposit = createApprovedDeposit(userUuid, coop = anotherCoop)
            testContext.deposits.add(anotherCoopDeposit)
        }

        verify("Cooperative can get approved user deposits") {
            val result = mockMvc.perform(
                get("$depositPath/approved")
                    .param("type", DepositWithdrawType.USER.name)
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(1)
            val response = deposits.deposits[0]
            assertThat(response.deposit.txHash).isNotNull()
            assertThat(response.user).isNotNull
            assertThat(response.deposit.coop).isEqualTo(COOP)
            assertThat(response.project).isNull()
            assertThat(response.deposit.documentResponse).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetApprovedProjectDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            val approved = createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.deposits = mutableListOf(unapproved, approved)
        }
        suppose("Project service will return project data") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }
        suppose("There is approved project deposit from another coop") {
            val anotherCoopDeposit =
                createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT, coop = anotherCoop)
            testContext.deposits.add(anotherCoopDeposit)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get approved project deposits") {
            val result = mockMvc.perform(
                get("$depositPath/approved")
                    .param("type", DepositWithdrawType.PROJECT.name)
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(1)
            val response = deposits.deposits[0]
            assertThat(response.project).isNotNull
            assertThat(response.deposit.coop).isEqualTo(COOP)
            assertThat(response.deposit.txHash).isNotNull()
            assertThat(response.project?.uuid).isEqualTo(projectUuid)
            assertThat(response.user?.uuid).isEqualTo(userUuid)
            assertThat(response.deposit.documentResponse).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetAllApprovedDeposits() {
        suppose("Approved and unapproved project and user deposits exist") {
            val unapprovedProject = createUnsignedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            val approvedProject = createApprovedDeposit(projectUuid, type = DepositWithdrawType.PROJECT)
            val unapprovedUser = createUnsignedDeposit(userUuid)
            val approvedUser = createApprovedDeposit(userUuid)
            testContext.deposits = mutableListOf(unapprovedProject, approvedProject, unapprovedUser, approvedUser)
        }
        suppose("Project service will return project data") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }
        suppose("There is unsigned project deposit from another coop") {
            val anotherCoopDeposit = createApprovedDeposit(
                projectUuid, type = DepositWithdrawType.PROJECT, coop = anotherCoop
            )
            testContext.deposits.add(anotherCoopDeposit)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get approved project and user deposits for unspecified type") {
            val result = mockMvc.perform(
                get("$depositPath/approved")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val deposits: DepositListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(2)
            assertThat(deposits.deposits.filter { it.deposit.coop != COOP }).hasSize(0)
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
            testContext.deposits = mutableListOf(approved)
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
            assertThat(transactionResponse.coop).isEqualTo(COOP)
        }
        verify("TransactionInfo for mint transaction is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos[0]
            assertThat(transactionInfo.companionData).isEqualTo(testContext.deposits.first().id.toString())
            assertThat(transactionInfo.type).isEqualTo(TransactionType.MINT)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
            assertThat(transactionInfo.coop).isEqualTo(COOP)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetDepositById() {
        suppose("User deposit exists") {
            val deposit = createUnsignedDeposit(userUuid)
            testContext.deposits = mutableListOf(deposit)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(setOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get deposit by id") {
            val savedDeposit = testContext.deposits.first()
            val depositId = savedDeposit.id
            val result = mockMvc.perform(
                get("$depositPath/$depositId")
            )
                .andExpect(status().isOk)
                .andReturn()

            val response: DepositWithDataServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.deposit.reference).isEqualTo(savedDeposit.reference)
            assertThat(response.deposit.id).isEqualTo(savedDeposit.id)
            assertThat(response.user).isNotNull
            assertThat(response.project).isNull()
        }
    }

    private class TestContext {
        val amount = 30_000L
        var deposits = mutableListOf<Deposit>()
        val documentLink = "document-link"
        lateinit var multipartFile: MockMultipartFile
        lateinit var transactionData: TransactionData
    }
}
