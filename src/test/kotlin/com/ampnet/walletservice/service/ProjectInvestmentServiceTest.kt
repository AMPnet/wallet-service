package com.ampnet.walletservice.service

import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.service.impl.ProjectInvestmentServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import com.ampnet.walletservice.service.pojo.ProjectInvestmentRequest
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class ProjectInvestmentServiceTest : JpaServiceTestBase() {

    private val projectInvestmentService: ProjectInvestmentService by lazy {
        val transactionService = TransactionInfoServiceImpl(transactionInfoRepository)
        ProjectInvestmentServiceImpl(walletRepository, mockedBlockchainService,
            mockedProjectService, transactionService)
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    fun mustThrowExceptionIfProjectIsNotActive() {
        suppose("Request is for inactive project") {
            testContext.investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 100)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid, active = false))
        }

        verify("Service will throw exception project not active") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_NOT_ACTIVE)
        }
    }

    @Test
    fun mustThrowExceptionIfProjectHasExpired() {
        suppose("Request is for expired project") {
            testContext.investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 100)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid, endDate = ZonedDateTime.now().minusYears(1)))
        }

        verify("Service will throw exception project expired") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE_EXPIRED)
        }
    }

    @Test
    fun mustThrowExceptionIfInvestmentAmountIsBelowMinimum() {
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }
        suppose("Request amount is below project minimum") {
            testContext.investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 10)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MIN_PER_USER)
        }
    }

    @Test
    fun mustThrowExceptionIfInvestmentAmountIsAboveMaximum() {
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }
        suppose("Request amount is about project maximum") {
            testContext.investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 10_000_000)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_PER_USER)
        }
    }

    @Test
    fun mustThrowExceptionIfUserDoesNotHaveWallet() {
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }
        verify("Service will throw exception that user wallet is missing") {
            val investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 100)
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.generateInvestInProjectTransaction(investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfProjectDoesNotHaveWallet() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, testContext.addressHash)
        }
        suppose("User has enough funds") {
            Mockito.`when`(mockedBlockchainService.getBalance(testContext.addressHash)).thenReturn(100_000_00)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }

        verify("Service will throw exception that project wallet is missing") {
            val investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 100)
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.generateInvestInProjectTransaction(investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfUserDoesNotEnoughFunds() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, testContext.addressHash)
        }
        suppose("User does not have enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 100)
            val userWalletHash = getWalletHash(userUuid)
            Mockito.`when`(mockedBlockchainService.getBalance(userWalletHash)).thenReturn(10)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_FUNDS)
        }
    }

    @Test
    fun mustBeAbleToGenerateInvestment() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, testContext.addressHash)
        }
        suppose("User does have enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 100_00)
            val userWalletHash = getWalletHash(userUuid)
            Mockito.`when`(mockedBlockchainService.getBalance(userWalletHash)).thenReturn(100_000_00)
        }
        suppose("Project has empty wallet") {
            val projectWallet = createWalletForProject(projectUuid, "project-wallet")
            val projectWalletHash = projectWallet.hash ?: fail("Wallet must be activated")
            Mockito.`when`(mockedBlockchainService.getBalance(projectWalletHash)).thenReturn(0)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }
        suppose("Blockchain service will generate transaction") {
            val userWalletHash = getWalletHash(userUuid)
            val projectWalletHash = getWalletHash(projectUuid)
            Mockito.`when`(mockedBlockchainService.generateProjectInvestmentTransaction(
                ProjectInvestmentTxRequest(userWalletHash, projectWalletHash, 100_00))
            ).thenReturn(testContext.transactionData)
        }

        verify("Service will generate transaction") {
            val transactionData = projectInvestmentService
                .generateInvestInProjectTransaction(testContext.investmentRequest)
            assertThat(transactionData.transactionData).isEqualTo(testContext.transactionData)
        }
    }

    @Test
    fun mustNotBeAbleToGenerateInvestmentIfProjectDidReachExpectedFunding() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, testContext.addressHash)
        }
        suppose("User does have enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(projectUuid, userUuid, 100_00)
            val userWalletHash = getWalletHash(userUuid)
            Mockito.`when`(mockedBlockchainService.getBalance(userWalletHash)).thenReturn(100_000_00)
        }
        suppose("Project service will return project") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid, expectedFunding = 10_000_000))
        }
        suppose("Project wallet has expected funding") {
            val projectWallet = createWalletForProject(projectUuid, "project-wallet")
            val projectWalletHash = projectWallet.hash ?: fail("Wallet must be activated")
            Mockito.`when`(mockedBlockchainService.getBalance(projectWalletHash)).thenReturn(10_000_000)
        }

        verify("Service will throw exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS)
        }
    }

    @Test
    fun mustBeAbleInvestInProject() {
        suppose("Blockchain service will return hash for post transaction") {
            Mockito.`when`(mockedBlockchainService
                    .postTransaction(testContext.signedTransaction)
            ).thenReturn(testContext.txHash)
        }

        verify("Service can post project invest transaction") {
            val txHash = projectInvestmentService.investInProject(testContext.signedTransaction)
            assertThat(txHash).isEqualTo(testContext.txHash)
        }
    }

    private class TestContext {
        lateinit var investmentRequest: ProjectInvestmentRequest
        val addressHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
        val signedTransaction = "SignedTransactionRequest"
        val transactionData = TransactionData("data")
        val txHash = "0x5432jlhkljkhsf78y7y23rekljhjksadhf6t4632ilhasdfh7836242hluafhds"
    }
}
