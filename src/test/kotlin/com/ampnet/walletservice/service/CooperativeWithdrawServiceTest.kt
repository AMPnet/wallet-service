package com.ampnet.walletservice.service

import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.impl.CooperativeWithdrawServiceImpl
import com.ampnet.walletservice.service.impl.StorageServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CooperativeWithdrawServiceTest : JpaServiceTestBase() {

    private val cooperativeWithdrawService: CooperativeWithdrawService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, mockedCloudStorageService)
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository, walletRepository)
        CooperativeWithdrawServiceImpl(
            walletRepository, withdrawRepository, mockedBlockchainService,
            transactionInfoService, storageServiceImpl, mockedMailService
        )
    }
    private lateinit var withdraw: Withdraw

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
    }

    /* Burn */
    @Test
    fun mustThrowExceptionForGeneratingBurnTxBeforeApproval() {
        suppose("User created withdraw") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for unapproved withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                cooperativeWithdrawService.generateBurnTransaction(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_NOT_APPROVED)
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingBurnTxForBurnedWithdraw() {
        suppose("Withdraw is burned") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for burned withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                cooperativeWithdrawService.generateBurnTransaction(withdraw.id, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_BURNED)
        }
    }

    @Test
    fun mustThrowExceptionForBurningUnapprovedWithdraw() {
        suppose("Withdraw is not approved") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to burn unapproved withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                cooperativeWithdrawService.burn(signedTransaction, withdraw.id)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_NOT_APPROVED)
        }
    }

    @Test
    fun mustThrowExceptionForBurningAlreadyBurnedWithdraw() {
        suppose("Withdraw is burned") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for burned withdraw") {
            val exception = assertThrows<InvalidRequestException> {
                cooperativeWithdrawService.burn(signedTransaction, withdraw.id)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_WITHDRAW_BURNED)
        }
    }
}
