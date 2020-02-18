package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.impl.StorageServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import com.ampnet.walletservice.service.impl.WithdrawServiceImpl
import com.ampnet.walletservice.service.pojo.WithdrawCreateServiceRequest
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired

class WithdrawServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var withdrawRepository: WithdrawRepository

    private val withdrawService: WithdrawService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, mockedCloudStorageService)
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        WithdrawServiceImpl(walletRepository, withdrawRepository, mockedBlockchainService, transactionInfoService,
                storageServiceImpl, mockedMailService, mockedProjectService)
    }
    private val bankAccount = "bank-account"
    private lateinit var withdraw: Withdraw

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
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
            assertThrows<InvalidRequestException> {
                withdrawService.deleteWithdraw(withdraw.id)
            }
        }
    }

    /* Approve */
    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForAnotherUser() {
        suppose("User created withdraw") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when another user tires to generate approval transaction") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForApprovedWithdraw() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate approval transaction for approved withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionForConfirmingApprovedTx() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to confirm already approved tx") {
            assertThrows<InvalidRequestException> {
                withdrawService.confirmApproval("signed-transaction", withdraw.id)
            }
        }
    }

    /* Burn */
    @Test
    fun mustThrowExceptionForGeneratingBurnTxBeforeApproval() {
        suppose("User created withdraw") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for unapproved withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateBurnTransaction(withdraw.id, userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingBurnTxForBurnedWithdraw() {
        suppose("Withdraw is burned") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for burned withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateBurnTransaction(withdraw.id, userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionForBurningUnapprovedWithdraw() {
        suppose("Withdraw is not approved") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to burn unapproved withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.burn(signedTransaction, withdraw.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionForBurningAlreadyBurnedWithdraw() {
        suppose("Withdraw is burned") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for burned withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.burn(signedTransaction, withdraw.id)
            }
        }
    }

    private fun createBurnedWithdraw(user: UUID, type: WalletType = WalletType.USER): Withdraw {
        val withdraw = Withdraw(0, user, 100L, ZonedDateTime.now(), user, bankAccount,
                "approved-tx", ZonedDateTime.now(),
                "burned-tx", ZonedDateTime.now(), UUID.randomUUID(), null, type)
        return withdrawRepository.save(withdraw)
    }

    private fun createApprovedWithdraw(user: UUID, type: WalletType = WalletType.USER): Withdraw {
        val withdraw = Withdraw(0, user, 100L, ZonedDateTime.now(), user, bankAccount,
                "approved-tx", ZonedDateTime.now(),
                null, null, null, null, type)
        return withdrawRepository.save(withdraw)
    }

    private fun createWithdraw(user: UUID, type: WalletType = WalletType.USER): Withdraw {
        val withdraw = Withdraw(0, user, 100L, ZonedDateTime.now(), user, bankAccount,
                null, null, null, null, null, null, type)
        return withdrawRepository.save(withdraw)
    }

    private fun createUserWithdrawServiceRequest() =
        WithdrawCreateServiceRequest(userUuid, bankAccount, 100L, userUuid, WalletType.USER)
}
