package com.ampnet.walletservice.service

import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.walletservice.controller.COOP
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.service.impl.CooperativeWalletServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.UUID

class CooperativeWalletServiceTest : JpaServiceTestBase() {

    private val walletAddress = "ak_RYkcTuYcyxQ6fWZsL2G3Kj3K5WCRUEXsi76bPUNkEsoHc52Wp"
    private val secondWalletAddress = "ak_2rTBMSCJgbeQoSt3MzSk93kAaYKjuTFyyfcMbhp62e2JJCTiSS"
    private val secondUser: UUID = UUID.randomUUID()
    private val newCoop = "new-coop"
    private lateinit var testContext: TestContext

    private val service: CooperativeWalletService by lazy {
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        CooperativeWalletServiceImpl(
            walletRepository, mockedUserService, mockedBlockchainService,
            transactionInfoService, mockedProjectService, mockedMailService
        )
    }

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    fun mustTransferOwnershipToAdmin() {
        suppose("There are two user wallets") {
            createWallet(userUuid, walletAddress, WalletType.USER)
            createWallet(secondUser, secondWalletAddress, WalletType.USER)
        }
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction, COOP))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return user address as token issuer and platform manager") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer(COOP))
                .thenReturn(walletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager(COOP))
                .thenReturn(walletAddress)
        }

        verify("Service will set user as admin") {
            service.updateCoopUserRoles(COOP)
            Mockito.verify(mockedUserService, Mockito.times(1))
                .setUserRole(userUuid, SetRoleRequest.Role.ADMIN, COOP)
        }
    }

    @Test
    fun mustTransferOwnershipToTokenIssuer() {
        suppose("There are two user wallets") {
            createWallet(userUuid, walletAddress, WalletType.USER)
            createWallet(secondUser, secondWalletAddress, WalletType.USER)
        }
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction, COOP))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return user address as token issuer") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer(COOP))
                .thenReturn(walletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager(COOP))
                .thenReturn(secondWalletAddress)
        }

        verify("Service will set user as token issuer") {
            service.updateCoopUserRoles(COOP)
            Mockito.verify(mockedUserService, Mockito.times(1))
                .setUserRole(userUuid, SetRoleRequest.Role.TOKEN_ISSUER, COOP)
        }
    }

    @Test
    fun mustTransferOwnershipToPlatformManager() {
        suppose("There are two user wallets") {
            createWallet(userUuid, walletAddress, WalletType.USER)
            createWallet(secondUser, secondWalletAddress, WalletType.USER)
        }
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction, COOP))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return user address as platform manager") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer(COOP))
                .thenReturn(secondWalletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager(COOP))
                .thenReturn(walletAddress)
        }

        verify("Service will set user as platform manager") {
            service.updateCoopUserRoles(COOP)
            Mockito.verify(mockedUserService, Mockito.times(1))
                .setUserRole(userUuid, SetRoleRequest.Role.PLATFORM_MANAGER, COOP)
        }
    }

    @Test
    fun mustNotTransferOwnershipToPlatformManagerFromAnotherCoop() {
        suppose("There are two user wallets") {
            createWallet(userUuid, walletAddress, WalletType.USER)
            createWallet(secondUser, secondWalletAddress, WalletType.USER)
        }
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction, newCoop))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return user address as platform manager") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer(newCoop))
                .thenReturn(secondWalletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager(newCoop))
                .thenReturn(walletAddress)
        }

        verify("Service will set user as platform manager") {
            assertThrows<ResourceNotFoundException> {
                service.updateCoopUserRoles(newCoop)
            }
            Mockito.verifyNoInteractions(mockedUserService)
        }
    }

    @Test
    fun mustBeAbleToActiveWallet() {
        suppose("Unactivated wallet exists") {
            val wallet = createWalletForUser(userUuid, defaultPublicKey)
            wallet.hash = null
            wallet.activatedAt = null
            testContext.wallet = walletRepository.save(wallet)
        }

        verify("Service can activated wallet") {
            val wallet = service.activateAdminWallet(
                testContext.wallet.activationData,
                testContext.wallet.coop,
                defaultAddressHash
            )
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
        }
        verify("Wallet is activated") {
            val wallet = walletRepository.findByActivationDataAndCoop(
                testContext.wallet.activationData, testContext.wallet.coop
            )
            assertThat(wallet.get().hash).isEqualTo(defaultAddressHash)
            assertThat(wallet.get().activatedAt).isNotNull()
        }
    }

    @Test
    fun mustNotBeAbleToActivatedAlreadyActivatedWallet() {
        suppose("User has activated wallet") {
            testContext.wallet = createWalletForUser(userUuid, defaultAddressHash)
        }

        verify("Service will throw exception that the wallet is already activated") {
            val exception = assertThrows<InvalidRequestException> {
                service.activateAdminWallet(
                    testContext.wallet.activationData, testContext.wallet.coop, defaultAddressHash
                )
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_HASH_EXISTS)
        }
    }

    @Test
    fun mustThrowExceptionForActivatingMissingWallet() {
        verify("Service will throw exception for missing wallet") {
            val exception = assertThrows<ResourceNotFoundException> {
                service.activateAdminWallet(defaultPublicKey, COOP, defaultAddressHash)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    private class TestContext {
        lateinit var wallet: Wallet
    }
}
