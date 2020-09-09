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
        CooperativeDepositServiceImpl(
            walletRepository, depositRepository, declinedRepository, mockedBlockchainService,
            transactionInfoService, storageServiceImpl, mockedMailService
        )
    }
    private lateinit var deposit: Deposit

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
    }

    @Test
    fun mustThrowExceptionIfReceivingUserDoesNotHaveWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWallets()
        }
        suppose("Unapproved deposit exist") {
            deposit = createApprovedDeposit(null)
        }

        verify("Service will throw exception for existing unapproved deposit") {
            assertThrows<ResourceNotFoundException> {
                val request = MintServiceRequest(deposit.id, createUserPrincipal(userUuid))
                cooperativeDepositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMissingForMintTransaction() {
        verify("Service will throw exception if the deposit is missing") {
            assertThrows<ResourceNotFoundException> {
                val request = MintServiceRequest(0, createUserPrincipal(userUuid))
                cooperativeDepositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMintedForMintTransaction() {
        suppose("Deposit is already minted") {
            deposit = createApprovedDeposit(txHash)
        }

        verify("Service will throw exception if the deposit already has tx hash") {
            assertThrows<ResourceAlreadyExistsException> {
                val request = MintServiceRequest(deposit.id, createUserPrincipal(userUuid))
                cooperativeDepositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsNotApprovedForMintTransaction() {
        suppose("Deposit is not approved") {
            deposit = createUnapprovedDeposit(userUuid)
        }

        verify("Service will throw exception if the deposit is not approved") {
            assertThrows<InvalidRequestException> {
                val request = MintServiceRequest(deposit.id, createUserPrincipal(userUuid))
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
            deposit = createApprovedDeposit(txHash)
        }

        verify("Service will throw exception if the deposit already has tx hash") {
            assertThrows<ResourceAlreadyExistsException> {
                cooperativeDepositService.confirmMintTransaction(signedTransaction, deposit.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsNotApprovedForConfirmMintTransaction() {
        suppose("Deposit is not approved") {
            deposit = createUnapprovedDeposit(userUuid)
        }

        verify("Service will throw exception if the deposit is not approved") {
            assertThrows<InvalidRequestException> {
                cooperativeDepositService.confirmMintTransaction(signedTransaction, deposit.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionForDecliningMissingDeposit() {
        verify("Service will throw exception for declining missing deposit") {
            assertThrows<ResourceNotFoundException> {
                cooperativeDepositService.decline(0, userUuid, "Missing")
            }
        }
    }

    @Test
    fun mustThrowExceptionForDecliningMintedDeposit() {
        suppose("Deposit is minted") {
            deposit = createApprovedDeposit(txHash)
        }

        verify("User cannot decline minted deposit") {
            assertThrows<InvalidRequestException> {
                cooperativeDepositService.decline(deposit.id, userUuid, "Minted")
            }
        }
    }
}
