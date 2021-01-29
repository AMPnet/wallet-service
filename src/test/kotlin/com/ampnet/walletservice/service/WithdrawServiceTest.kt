package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.impl.BankAccountServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import com.ampnet.walletservice.service.impl.WithdrawServiceImpl
import com.ampnet.walletservice.service.pojo.request.WithdrawCreateServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.UUID

class WithdrawServiceTest : JpaServiceTestBase() {

    private val withdrawService: WithdrawService by lazy {
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        val bankAccountService = BankAccountServiceImpl(bankAccountRepository)
        WithdrawServiceImpl(
            walletRepository, withdrawRepository, mockedBlockchainService, transactionInfoService,
            mockedMailService, mockedProjectService, bankAccountService
        )
    }
    private val userPrincipal = createUserPrincipal(userUuid)
    private lateinit var withdraw: Withdraw

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
    }

    /* Get */
    @Test
    fun mustThrowExceptionIfUserNotProjectOwnerGetWithdraw() {
        suppose("Project has created withdraw") {
            createWithdraw(projectUuid, DepositWithdrawType.PROJECT)
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

    @Test
    fun mustReturnBurnedWithdrawAsPending() {
        suppose("Project has created withdraw") {
            withdraw = createBurnedWithdraw(projectUuid, DepositWithdrawType.PROJECT)
        }
        suppose("Project service will return project") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("Service will return burned withdraw as pending") {
            val pendingWithdraw = withdrawService.getPendingForProject(projectUuid, userUuid)
            assertThat(pendingWithdraw?.id).isEqualTo(withdraw.id)
        }
    }

    @Test
    fun mustReturnApprovedWithdrawAsPending() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid, DepositWithdrawType.USER)
        }

        verify("Service will return approved withdraw as pending") {
            val pendingWithdraw = withdrawService.getPendingForOwner(userUuid)
            assertThat(pendingWithdraw?.id).isEqualTo(withdraw.id)
        }
    }

    /* Create */
    @Test
    fun mustThrowExceptionForInvalidIban() {
        verify("Service will throw exception for invalid IBAN") {
            val request = WithdrawCreateServiceRequest(
                userUuid, "ivalid-iban", 100L, createUserPrincipal(userUuid), DepositWithdrawType.USER
            )
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.createWithdraw(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_BANK_INVALID)
        }
    }

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
            databaseCleanerService.deleteAllWallets()
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
                    projectUuid, bankAccount, 100L, createUserPrincipal(userUuid), DepositWithdrawType.PROJECT
                )
                withdrawService.createWithdraw(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }

    /* Delete */
    @Test
    fun deleteApprovedWithdraw() {
        suppose("Burned withdraw is created") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("User can delete approved withdraw") {
            withdrawService.deleteWithdraw(withdraw.id, userPrincipal)
        }
        verify("Withdraw is deleted") {
            assertThat(withdrawRepository.findById(withdraw.id)).isNotPresent
        }
    }

    @Test
    fun mustThrowExceptionForDeletingBurnedWithdraw() {
        suppose("Burned withdraw is created") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to delete burned withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.deleteWithdraw(withdraw.id, userPrincipal)
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
                withdrawService.deleteWithdraw(withdraw.id, userPrincipal)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForDeletingOtherProjectWithdraw() {
        suppose("Project created withdraw") {
            withdraw = createWithdraw(projectUuid, DepositWithdrawType.PROJECT)
        }
        suppose("Project service will return project") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, userUuid))
        }

        verify("Service will throw exception when user tries to delete others project withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.deleteWithdraw(withdraw.id, createUserPrincipal(UUID.randomUUID()))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
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
                withdrawService.generateApprovalTransaction(withdraw.id, createUserPrincipal(UUID.randomUUID()))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForNonAdminProject() {
        suppose("Project has withdraw") {
            withdraw = createWithdraw(projectUuid, type = DepositWithdrawType.PROJECT)
        }
        suppose("Project service will return project with another user") {
            Mockito.`when`(mockedProjectService.getProject(projectUuid))
                .thenReturn(createProjectResponse(projectUuid, UUID.randomUUID()))
        }

        verify("Service will throw exception when user tires to generate approval transaction for project") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, createUserPrincipal(userUuid))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING_PRIVILEGE)
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForApprovedWithdraw() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate approval transaction for approved withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, createUserPrincipal(userUuid))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_APPROVED)
        }
    }

    @Test
    fun mustThrowExceptionForConfirmingApprovedTx() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to confirm already approved tx") {
            val exception = assertThrows<InvalidRequestException> {
                withdrawService.confirmApproval("signed-transaction", withdraw.id, withdraw.coop)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_APPROVED)
        }
    }

    private fun createUserWithdrawServiceRequest() =
        WithdrawCreateServiceRequest(userUuid, bankAccount, 100L, createUserPrincipal(userUuid), DepositWithdrawType.USER)
}
