package com.ampnet.walletservice.service

import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.persistence.repository.RevenuePayoutRepository
import com.ampnet.walletservice.service.impl.RevenueServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired

class RevenueServiceTest : JpaServiceTestBase() {

    @Autowired
    private lateinit var revenuePayoutRepository: RevenuePayoutRepository

    private val revenueService: RevenueService by lazy {
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        RevenueServiceImpl(revenuePayoutRepository, walletRepository,
            mockedProjectService, mockedBlockchainService, transactionInfoService)
    }
    private val projectWallet = "project-wallet"

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
    }

    @Test
    fun mustThrowExceptionIfProjectDoesNotHaveEnoughFunds() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, "user-wallet")
        }
        suppose("Project has a wallet with enough funds") {
            createWalletForProject(projectUuid, projectWallet)
            Mockito.`when`(mockedBlockchainService.getBalance(projectWallet))
                .thenReturn(100L)
        }
        suppose("Project service will return project for user") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }

        verify("Service will throw exception for missing funds") {
            val exception = assertThrows<InvalidRequestException> {
                revenueService.generateRevenuePayout(userUuid, projectUuid, 101L)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_FUNDS)
        }
    }

    @Test
    fun mustThrowExceptionIfTheUserIsNotProjectAdmin() {
        suppose("Project service will return project with other user as admin") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(getProjectResponse(projectUuid, UUID.randomUUID(), organizationUuid))
        }

        verify("Service will throw exception for missing project privileges") {
            val exception = assertThrows<InvalidRequestException> {
                revenueService.generateRevenuePayout(userUuid, projectUuid, 100L)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }
}
