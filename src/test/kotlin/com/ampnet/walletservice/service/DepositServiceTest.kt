package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.impl.DepositServiceImpl
import com.ampnet.walletservice.service.pojo.DepositCreateServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.UUID

class DepositServiceTest : JpaServiceTestBase() {

    private val depositService: DepositService by lazy {
        DepositServiceImpl(walletRepository, depositRepository, mockedProjectService)
    }
    private lateinit var deposit: Deposit

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
            createUnsigned(userUuid)
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
                    projectUuid, userUuid, 100L, DepositWithdrawType.PROJECT
                )
                depositService.create(serviceRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForDeletingMintedDeposit() {
        suppose("Deposit is minted") {
            deposit = createApprovedDeposit(txHash)
        }

        verify("User cannot delete minted deposit") {
            assertThrows<InvalidRequestException> {
                depositService.delete(deposit.id, userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionForDeletingNonExistingDeposit() {
        verify("User cannot delete non existing deposit") {
            val exception = assertThrows<ResourceNotFoundException> {
                depositService.delete(0, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_DEPOSIT_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionForDeletingOtherProjectWithdraw() {
        suppose("Project created withdraw") {
            deposit = createUnsigned(projectUuid, DepositWithdrawType.PROJECT)
        }
        suppose("Project service will return project") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, UUID.randomUUID()))
        }

        verify("Service will throw exception when user tries to delete others project withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                depositService.delete(deposit.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }
}
