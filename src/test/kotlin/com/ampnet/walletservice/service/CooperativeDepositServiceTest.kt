package com.ampnet.walletservice.service

import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.impl.CooperativeDepositServiceImpl
import com.ampnet.walletservice.service.impl.StorageServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CooperativeDepositServiceTest : JpaServiceTestBase() {

    private val cooperativeDepositService: CooperativeDepositService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, mockedCloudStorageService)
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        CooperativeDepositServiceImpl(walletRepository, depositRepository, mockedBlockchainService,
            transactionInfoService, storageServiceImpl, mockedMailService)
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
        testContext = TestContext()
    }

    @Test
    fun mustThrowExceptionIfReceivingUserDoesNotHaveWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWallets()
        }
        suppose("Unapproved deposit exist") {
            testContext.deposit = createApprovedDeposit(null)
        }

        verify("Service will throw exception for existing unapproved deposit") {
            assertThrows<ResourceNotFoundException> {
                val request = MintServiceRequest(testContext.deposit.id, userUuid)
                cooperativeDepositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMissingForMintTransaction() {
        verify("Service will throw exception if the deposit is missing") {
            assertThrows<ResourceNotFoundException> {
                val request = MintServiceRequest(0, userUuid)
                cooperativeDepositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMintedForMintTransaction() {
        suppose("Deposit is already minted") {
            testContext.deposit = createApprovedDeposit(txHash)
        }

        verify("Service will throw exception if the deposit already has tx hash") {
            assertThrows<ResourceAlreadyExistsException> {
                val request = MintServiceRequest(testContext.deposit.id, userUuid)
                cooperativeDepositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsNotApprovedForMintTransaction() {
        suppose("Deposit is not approved") {
            testContext.deposit = createUnapprovedDeposit()
        }

        verify("Service will throw exception if the deposit is not approved") {
            assertThrows<InvalidRequestException> {
                val request = MintServiceRequest(testContext.deposit.id, userUuid)
                cooperativeDepositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMissingForConfirmMintTransaction() {
        verify("Service will throw exception if the deposit is missing") {
            assertThrows<ResourceNotFoundException> {
                cooperativeDepositService.confirmMintTransaction(signedTransaction, 0)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMintedForConfirmMintTransaction() {
        suppose("Deposit is already minted") {
            testContext.deposit = createApprovedDeposit(txHash)
        }

        verify("Service will throw exception if the deposit already has tx hash") {
            assertThrows<ResourceAlreadyExistsException> {
                cooperativeDepositService.confirmMintTransaction(signedTransaction, testContext.deposit.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsNotApprovedForConfirmMintTransaction() {
        suppose("Deposit is not approved") {
            testContext.deposit = createUnapprovedDeposit()
        }

        verify("Service will throw exception if the deposit is not approved") {
            assertThrows<InvalidRequestException> {
                cooperativeDepositService.confirmMintTransaction(signedTransaction, testContext.deposit.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionForDeletingMintedDeposit() {
        suppose("Deposit is minted") {
            testContext.deposit = createApprovedDeposit(txHash)
        }

        verify("User cannot delete minted deposit") {
            assertThrows<InvalidRequestException> {
                cooperativeDepositService.delete(testContext.deposit.id)
            }
        }
    }

    private class TestContext {
        lateinit var deposit: Deposit
    }
}
