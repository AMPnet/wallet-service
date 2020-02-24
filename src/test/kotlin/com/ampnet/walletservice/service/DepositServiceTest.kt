package com.ampnet.walletservice.service

import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.service.impl.DepositServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DepositServiceTest : JpaServiceTestBase() {

    private val depositService: DepositService by lazy {
        DepositServiceImpl(walletRepository, depositRepository, mockedMailService)
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
            assertThrows<ResourceAlreadyExistsException> {
                depositService.create(userUuid, 100L)
            }
        }
    }
}
