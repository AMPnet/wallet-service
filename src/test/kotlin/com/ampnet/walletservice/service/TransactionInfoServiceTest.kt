package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionInfoServiceTest : JpaServiceTestBase() {

    private val transactionInfoService: TransactionInfoService by lazy {
        TransactionInfoServiceImpl(transactionInfoRepository, walletRepository)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllTransactionInfo()
    }

    @Test
    fun mustCreateOrgTransaction() {
        suppose("Service can create org transactionInfo") {
            testContext.transactionInfo =
                transactionInfoService.createOrgTransaction(organizationUuid, testContext.organizationName, userUuid)
        }

        verify("Org transactionInfo is created") {
            val optionalTx = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.CREATE_ORG)
            assertThat(tx.userUuid).isEqualTo(userUuid)
            assertThat(tx.companionData).contains(organizationUuid.toString())
            assertThat(tx.description).contains(testContext.organizationName)
        }
    }

    @Test
    fun mustCreateProjectTransaction() {
        suppose("Service can create project transactionInfo") {
            testContext.transactionInfo =
                transactionInfoService.createProjectTransaction(projectUuid, testContext.projectName, userUuid)
        }

        verify("Project transactionInfo is created") {
            val optionalTx = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.CREATE_PROJECT)
            assertThat(tx.userUuid).isEqualTo(userUuid)
            assertThat(tx.companionData).contains(projectUuid.toString())
            assertThat(tx.description).contains(testContext.projectName)
        }
    }

    @Test
    fun mustCreateInvestAllowanceTransaction() {
        suppose("Service can create invest allowance transactionInfo") {
            testContext.transactionInfo = transactionInfoService.createInvestTransaction(
                testContext.projectName, testContext.amount, userUuid
            )
        }

        verify("Invest allowance transactionInfo is created") {
            val optionalTx = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.INVEST)
            assertThat(tx.userUuid).isEqualTo(userUuid)
            assertThat(tx.description).contains(testContext.projectName)
            assertThat(tx.description).contains("100.23")
        }
    }

    private class TestContext {
        val amount = 100_23L
        lateinit var transactionInfo: TransactionInfo
        val projectName = "Das Project"
        val organizationName = "ZEF"
    }
}
