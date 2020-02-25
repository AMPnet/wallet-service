package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.service.impl.DepositServiceImpl
import com.ampnet.walletservice.service.pojo.DepositCreateServiceRequest
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class DepositServiceTest : JpaServiceTestBase() {

    private val depositService: DepositService by lazy {
        DepositServiceImpl(walletRepository, depositRepository, mockedMailService, mockedProjectService)
    }

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
    }

    @Test
    fun mustThrowExceptionIfUnapprovedDepositExistsForCreatingDeposit() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(userUuid, "wallet-hash")
        }
        suppose("Unapproved and approved deposits exists") {
            createUnapprovedDeposit()
            createApprovedDeposit(txHash)
        }

        verify("Service will throw exception for existing unapproved deposit") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                val serviceRequest = DepositCreateServiceRequest(userUuid, userUuid, 100L, DepositWithdrawType.USER)
                depositService.create(serviceRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_DEPOSIT_EXISTS)
        }
    }

    @Test
    fun mustThrowExceptionIfUserIsNotProjectOwner() {
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForProject(projectUuid, "project-wallet-hash")
        }
        suppose("Project service will return project data") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, UUID.randomUUID()))
        }

        verify("Service will throw exception for missing project privileges by user") {
            val exception = assertThrows<InvalidRequestException> {
                val serviceRequest = DepositCreateServiceRequest(
                    projectUuid, userUuid, 100L, DepositWithdrawType.PROJECT)
                depositService.create(serviceRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }
}
