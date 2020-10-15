package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.enums.PrivilegeType
import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.security.WithMockCrowdfoundUser
import com.ampnet.walletservice.service.pojo.WithdrawListServiceResponse
import com.ampnet.walletservice.service.pojo.WithdrawServiceResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class CooperativeWithdrawControllerTest : ControllerTestBase() {

    private val withdrawPath = "/cooperative/withdraw"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetApprovedUserWithdraws() {
        suppose("Approved and unapproved user withdraws are created") {
            val approvedWithdraw = createApprovedWithdraw(userUuid)
            val secondApprovedWithdraw = createApprovedWithdraw(userUuid)
            val unapprovedWithdraw = createWithdraw(userUuid)
            testContext.withdraws = listOf(approvedWithdraw, secondApprovedWithdraw, unapprovedWithdraw)
        }
        suppose("Some project has approved withdraw") {
            createApprovedWithdraw(UUID.randomUUID(), type = DepositWithdrawType.PROJECT)
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get list of approved user withdraws") {
            val result = mockMvc.perform(
                get("$withdrawPath/approved")
                    .param("size", "1")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val withdrawList: WithdrawListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdrawWithData = withdrawList.withdraws.first()
            val withdraw = withdrawWithData.withdraw
            val project = withdrawWithData.project
            val user = withdrawWithData.user
            assertThat(project).isNull()
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.id).isNotNull()
            assertThat(withdraw.bankAccount).isNotNull()
            assertThat(withdraw.approvedTxHash).isEqualTo(testContext.approvedTx)
            assertThat(withdraw.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(user?.uuid).isEqualTo(userUuid)
            assertThat(withdrawWithData.walletHash).isEqualTo(walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.burnedAt).isNull()
            assertThat(withdraw.burnedBy).isNull()
            assertThat(withdraw.burnedTxHash).isNull()
            assertThat(withdraw.documentResponse).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetApprovedProjectWithdraws() {
        suppose("Approved and unapproved project withdraws are created") {
            val approvedWithdraw = createApprovedWithdraw(projectUuid, type = DepositWithdrawType.PROJECT)
            val secondApprovedWithdraw = createApprovedWithdraw(projectUuid, type = DepositWithdrawType.PROJECT)
            val unapprovedWithdraw = createWithdraw(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.withdraws = listOf(approvedWithdraw, secondApprovedWithdraw, unapprovedWithdraw)
        }
        suppose("Some user has approved withdraw") {
            createApprovedWithdraw(UUID.randomUUID())
        }
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project service will return project data") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get list of approved project withdraws") {
            val result = mockMvc.perform(
                get("$withdrawPath/approved/project")
                    .param("size", "1")
                    .param("page", "0")
                    .param("sort", "approvedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val withdrawList: WithdrawListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdrawWithData = withdrawList.withdraws.first()
            val withdraw = withdrawWithData.withdraw
            val project = withdrawWithData.project
            val user = withdrawWithData.user
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.id).isNotNull()
            assertThat(withdraw.bankAccount).isNotNull()
            assertThat(withdraw.approvedTxHash).isEqualTo(testContext.approvedTx)
            assertThat(withdraw.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(project?.uuid).isEqualTo(projectUuid.toString())
            assertThat(withdrawWithData.walletHash).isEqualTo(walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.burnedAt).isNull()
            assertThat(withdraw.burnedBy).isNull()
            assertThat(withdraw.burnedTxHash).isNull()
            assertThat(user?.uuid).isEqualTo(userUuid)
            assertThat(withdraw.documentResponse).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetBurnedUserWithdraws() {
        suppose("Approved and burned user withdraws are created") {
            val approvedWithdraw = createApprovedWithdraw(userUuid)
            val burnedWithdraw = createBurnedWithdraw(userUuid)
            testContext.withdraws = listOf(approvedWithdraw, burnedWithdraw)
        }
        suppose("Some project has burned withdraw") {
            createBurnedWithdraw(UUID.randomUUID(), type = DepositWithdrawType.PROJECT)
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get list of burned user withdraws") {
            val result = mockMvc.perform(
                get("$withdrawPath/burned")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "burnedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val withdrawList: WithdrawListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdrawWithData = withdrawList.withdraws.first()
            val withdraw = withdrawWithData.withdraw
            val project = withdrawWithData.project
            val user = withdrawWithData.user
            assertThat(project).isNull()
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.id).isNotNull()
            assertThat(withdraw.bankAccount).isEqualTo(testContext.bankAccount)
            assertThat(withdraw.approvedTxHash).isEqualTo(testContext.approvedTx)
            assertThat(withdraw.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(user?.uuid).isEqualTo(userUuid)
            assertThat(withdrawWithData.walletHash).isEqualTo(walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.burnedAt).isNotNull()
            assertThat(withdraw.burnedBy).isNotNull()
            assertThat(withdraw.burnedTxHash).isEqualTo(testContext.burnedTx)
            assertThat(withdraw.documentResponse).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetBurnedProjectWithdraws() {
        suppose("Approved and burned project withdraws are created") {
            val approvedWithdraw = createApprovedWithdraw(projectUuid, type = DepositWithdrawType.PROJECT)
            val burnedWithdraw = createBurnedWithdraw(projectUuid, type = DepositWithdrawType.PROJECT)
            testContext.withdraws = listOf(approvedWithdraw, burnedWithdraw)
        }
        suppose("Some user has burned withdraw") {
            createBurnedWithdraw(UUID.randomUUID(), type = DepositWithdrawType.USER)
        }
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, walletHash)
        }
        suppose("Project service will return project") {
            Mockito.`when`(projectService.getProjects(setOf(projectUuid)))
                .thenReturn(listOf(createProjectResponse(projectUuid, userUuid)))
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(setOf(userUuid)))
                .thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Cooperative can get list of burned project withdraws") {
            val result = mockMvc.perform(
                get("$withdrawPath/burned/project")
                    .param("size", "20")
                    .param("page", "0")
                    .param("sort", "burnedAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val withdrawList: WithdrawListServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdrawWithData = withdrawList.withdraws.first()
            val withdraw = withdrawWithData.withdraw
            val project = withdrawWithData.project
            val user = withdrawWithData.user
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.id).isNotNull()
            assertThat(withdraw.bankAccount).isEqualTo(testContext.bankAccount)
            assertThat(withdraw.approvedTxHash).isEqualTo(testContext.approvedTx)
            assertThat(withdraw.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(project?.uuid).isEqualTo(projectUuid.toString())
            assertThat(withdrawWithData.walletHash).isEqualTo(walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.burnedAt).isNotNull()
            assertThat(withdraw.burnedBy).isNotNull()
            assertThat(withdraw.burnedTxHash).isEqualTo(testContext.burnedTx)
            assertThat(withdraw.documentResponse).isNull()
            assertThat(user?.uuid).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToGetWithdrawListWithUserRole() {
        verify("User will get forbidden for accessing cooperative path") {
            mockMvc.perform(get("$withdrawPath/approved"))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WITHDRAW])
    fun mustBeAbleToGenerateBurnTransaction() {
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User has created approved withdraw") {
            testContext.withdraw = createApprovedWithdraw(userUuid)
        }
        suppose("Blockchain service will return approve burn transaction") {
            testContext.transactionData = TransactionData("approve-burn-transaction")
            Mockito.`when`(blockchainService.generateBurnTransaction(walletHash))
                .thenReturn(testContext.transactionData)
        }

        verify("User can generate burn transaction") {
            val result = mockMvc.perform(
                post("$withdrawPath/${testContext.withdraw.id}/transaction/burn")
            )
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData.tx)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.BURN)
        }
        verify("TransactionInfo for burn is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos.first()
            assertThat(transactionInfo.companionData).isEqualTo(testContext.withdraw.id.toString())
            assertThat(transactionInfo.type).isEqualTo(TransactionType.BURN)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WITHDRAW])
    fun mustBeAbleToUploadDocument() {
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, walletHash)
        }
        suppose("User has created approved withdraw") {
            testContext.withdraw = createApprovedWithdraw(userUuid)
        }
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile(
                "file", "test.txt", "text/plain", "DocumentData".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes
                )
            ).thenReturn(testContext.documentLink)
        }

        verify("Cooperative can add document for withdraw") {
            val result = mockMvc.perform(
                fileUpload("$withdrawPath/${testContext.withdraw.id}/document").file(testContext.multipartFile)
            )
                .andExpect(status().isOk)
                .andReturn()

            val withdrawResponse: WithdrawServiceResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.id).isEqualTo(testContext.withdraw.id)
            assertThat(withdrawResponse.documentResponse?.link).isEqualTo(testContext.documentLink)
        }
    }

    private fun createBurnedWithdraw(owner: UUID, type: DepositWithdrawType = DepositWithdrawType.USER): Withdraw {
        val document = saveFile("withdraw-doc", "doc-link", "type", 1, userUuid)
        val withdraw = Withdraw(
            0, owner, testContext.amount, ZonedDateTime.now(), userUuid, testContext.bankAccount,
            testContext.approvedTx, ZonedDateTime.now(),
            testContext.burnedTx, ZonedDateTime.now(), UUID.randomUUID(), document, type
        )
        return withdrawRepository.save(withdraw)
    }

    private class TestContext {
        val amount = 1000L
        val bankAccount = "AL35202111090000000001234567"
        val approvedTx = "approved-tx"
        val burnedTx = "burned-tx"
        val documentLink = "doc-link"
        var withdraws = listOf<Withdraw>()
        lateinit var withdraw: Withdraw
        lateinit var transactionData: TransactionData
        lateinit var multipartFile: MockMultipartFile
    }
}
