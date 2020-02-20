package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InternalException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import com.ampnet.walletservice.service.impl.WithdrawServiceImpl
import com.ampnet.walletservice.service.pojo.WithdrawCreateServiceRequest
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class WithdrawServiceTest : JpaServiceTestBase() {

    private val withdrawService: WithdrawService by lazy {
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        WithdrawServiceImpl(walletRepository, withdrawRepository, mockedBlockchainService, transactionInfoService,
            mockedMailService, mockedProjectService)
    }
    private lateinit var withdraw: Withdraw

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
    }

    /* Get */
    @Test
    fun mustThrowExceptionIfUserNotProjectOwnerGetWithdraw() {
        suppose("Project has created withdraw") {
            createWithdraw(projectUuid, WalletType.PROJECT)
        }
        suppose("Project service will return project") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("Service will throw exception user is missing project privilege") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.getPendingForProject(projectUuid, UUID.randomUUID())
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }

    /* Create */
    @Test
    fun mustThrowExceptionIfUserHasUnapprovedWithdraw() {
        suppose("User has created withdraw") {
            createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to create new withdraw") {
            assertThrows<ResourceAlreadyExistsException> {
                withdrawService.createWithdraw(createUserWithdrawServiceRequest())
            }
        }
    }

    @Test
    fun mustThrowExceptionIfUserHasApprovedWithdraw() {
        suppose("User has approved withdraw") {
            createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to create new withdraw") {
            assertThrows<ResourceAlreadyExistsException> {
                withdrawService.createWithdraw(createUserWithdrawServiceRequest())
            }
        }
    }

    @Test
    fun mustThrowExceptionIfUserDoesNotHaveEnoughFunds() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, "default-address")
        }
        suppose("User does not have enough funds") {
            Mockito.`when`(mockedBlockchainService.getBalance(getWalletHash(userUuid))).thenReturn(99L)
        }

        verify("Service will throw exception for insufficient funds") {
            assertThrows<InvalidRequestException> {
                withdrawService.createWithdraw(createUserWithdrawServiceRequest())
            }
        }
    }

    @Test
    fun mustThrowExceptionIfUserIsNotProjectOwner() {
        suppose("Project has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForUser(projectUuid, "default-address")
        }
        suppose("Project has enough funds") {
            Mockito.`when`(mockedBlockchainService.getBalance(getWalletHash(projectUuid))).thenReturn(100L)
        }
        suppose("Project service will return project") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, UUID.randomUUID()))
        }

        verify("Service will throw exception missing privilege for project") {
            val exception = assertThrows<InvalidRequestException> {
                val request = WithdrawCreateServiceRequest(
                    projectUuid, bankAccount, 100L, userUuid, WalletType.PROJECT)
                withdrawService.createWithdraw(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }

    /* Delete */
    @Test
    fun mustThrowExceptionForDeletingBurnedWithdraw() {
        suppose("Burned withdraw is created") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to delete burned withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.deleteWithdraw(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_BURNED)
        }
    }

    @Test
    fun mustThrowExceptionForDeletingOthersWithdraw() {
        suppose("User created withdraw") {
            withdraw = createWithdraw(UUID.randomUUID())
        }

        verify("Service will throw exception when user tries to delete others withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.deleteWithdraw(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForDeletingOtherProjectWithdraw() {
        suppose("Project created withdraw") {
            withdraw = createWithdraw(projectUuid, WalletType.PROJECT)
        }
        suppose("Project service will return project") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("Service will throw exception when user tries to delete others project withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.deleteWithdraw(withdraw.id, UUID.randomUUID())
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForDeletingOrganizationWithdraw() {
        suppose("Organization created withdraw") {
            withdraw = createWithdraw(organizationUuid, WalletType.ORG)
        }

        verify("Service will throw exception when user tries to delete others project withdraw") {
            val exception = assertThrows<InternalException> {
                withdrawService.deleteWithdraw(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.INT_INVALID_VALUE)
        }
    }

    /* Approve */
    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForAnotherUser() {
        suppose("User created withdraw") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when another user tires to generate approval transaction") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, UUID.randomUUID())
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForNonAdminProject() {
        suppose("Project has withdraw") {
            withdraw = createWithdraw(projectUuid, type = WalletType.PROJECT)
        }
        suppose("Project service will return project with another user") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, UUID.randomUUID()))
        }

        verify("Service will throw exception when user tires to generate approval transaction for project") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForOrganization() {
        suppose("Organization created withdraw") {
            withdraw = createWithdraw(organizationUuid, WalletType.ORG)
        }

        verify("Service will throw exception when another user tires to generate approval transaction") {
            val exception = assertThrows<InternalException> {
                withdrawService.generateApprovalTransaction(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.INT_INVALID_VALUE)
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForApprovedWithdraw() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate approval transaction for approved withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_APPROVED)
        }
    }

    private fun createUserWithdrawServiceRequest() =
        WithdrawCreateServiceRequest(userUuid, bankAccount, 100L, userUuid, WalletType.USER)
}
