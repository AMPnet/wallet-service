package com.ampnet.walletservice.service

import com.ampnet.crowdfunding.proto.TransactionState
import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.walletservice.controller.COOP
import com.ampnet.walletservice.enums.TransferWalletType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.service.impl.CooperativeWalletServiceImpl
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import com.ampnet.walletservice.service.pojo.TransferOwnershipRequest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.UUID
import java.util.concurrent.TimeUnit

class CooperativeWalletServiceTest : JpaServiceTestBase() {

    private val walletAddress = "ak_RYkcTuYcyxQ6fWZsL2G3Kj3K5WCRUEXsi76bPUNkEsoHc52Wp"
    private val secondWalletAddress = "ak_2rTBMSCJgbeQoSt3MzSk93kAaYKjuTFyyfcMbhp62e2JJCTiSS"
    private val secondUser: UUID = UUID.randomUUID()

    @MockBean
    private lateinit var mockedUserService: UserService
    private val service: CooperativeWalletService by lazy {
        databaseCleanerService.deleteAllWallets()
        createWallet(userUuid, walletAddress, WalletType.USER)
        createWallet(secondUser, secondWalletAddress, WalletType.USER)
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        CooperativeWalletServiceImpl(
            walletRepository, mockedUserService, mockedBlockchainService,
            transactionInfoService, mockedProjectService, applicationProperties
        )
    }

    @Test
    fun mustTransferOwnershipToAdmin() {
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return transaction status") {
            Mockito.`when`(mockedBlockchainService.getTransactionState(txHash))
                .thenReturn(TransactionState.MINED)
        }
        suppose("Blockchain service will return user address as token issuer and platform manager") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer(COOP))
                .thenReturn(walletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager(COOP))
                .thenReturn(walletAddress)
        }

        verify("Service will set user as admin") {
            val request = TransferOwnershipRequest(
                secondUser, walletAddress, TransferWalletType.TOKEN_ISSUER, signedTransaction, COOP
            )
            service.transferOwnership(request)
            await().atLeast(applicationProperties.grpc.blockchainPollingDelay * 5, TimeUnit.MILLISECONDS)
                .until { true }
            Mockito.verify(mockedUserService, Mockito.times(1))
                .setUserRole(userUuid, SetRoleRequest.Role.ADMIN, COOP)
        }
    }

    @Test
    fun mustTransferOwnershipToTokenIssuer() {
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return transaction status") {
            Mockito.`when`(mockedBlockchainService.getTransactionState(txHash))
                .thenReturn(TransactionState.MINED)
        }
        suppose("Blockchain service will return user address as token issuer") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer(COOP))
                .thenReturn(walletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager(COOP))
                .thenReturn(secondWalletAddress)
        }

        verify("Service will set user as token issuer") {
            val request = TransferOwnershipRequest(
                secondUser, walletAddress, TransferWalletType.TOKEN_ISSUER, signedTransaction, COOP
            )
            service.transferOwnership(request)
            await().pollDelay(applicationProperties.grpc.blockchainPollingDelay * 5, TimeUnit.MILLISECONDS)
                .until { true }
            Mockito.verify(mockedUserService, Mockito.times(1))
                .setUserRole(userUuid, SetRoleRequest.Role.TOKEN_ISSUER, COOP)
        }
    }

    @Test
    fun mustTransferOwnershipToPlatformManager() {
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return transaction status") {
            Mockito.`when`(mockedBlockchainService.getTransactionState(txHash))
                .thenReturn(TransactionState.MINED)
        }
        suppose("Blockchain service will return user address as platform manager") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer(COOP))
                .thenReturn(secondWalletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager(COOP))
                .thenReturn(walletAddress)
        }

        verify("Service will set user as platform manager") {
            val request = TransferOwnershipRequest(
                secondUser, walletAddress, TransferWalletType.PLATFORM_MANAGER, signedTransaction, COOP
            )
            service.transferOwnership(request)
            await().pollDelay(applicationProperties.grpc.blockchainPollingDelay * 5, TimeUnit.MILLISECONDS)
                .until { true }
            Mockito.verify(mockedUserService, Mockito.times(1))
                .setUserRole(userUuid, SetRoleRequest.Role.PLATFORM_MANAGER, COOP)
        }
    }

    @Test
    fun mustNotTransferOwnershipToPlatformManagerFromAnotherCoop() {
        suppose("Blockchain service will handle post transaction") {
            Mockito.`when`(mockedBlockchainService.postTransaction(signedTransaction))
                .thenReturn(txHash)
        }
        suppose("Blockchain service will return transaction status") {
            Mockito.`when`(mockedBlockchainService.getTransactionState(txHash))
                .thenReturn(TransactionState.MINED)
        }
        suppose("Blockchain service will return user address as platform manager") {
            Mockito.`when`(mockedBlockchainService.getTokenIssuer("new-coop"))
                .thenReturn(secondWalletAddress)
            Mockito.`when`(mockedBlockchainService.getPlatformManager("new-coop"))
                .thenReturn(walletAddress)
        }

        verify("Service will set user as platform manager") {
            val request = TransferOwnershipRequest(
                secondUser, walletAddress, TransferWalletType.PLATFORM_MANAGER, signedTransaction, "new-coop"
            )
            service.transferOwnership(request)
            assertThrows<InvalidRequestException> {
                service.transferOwnership(request)
                await().pollDelay(applicationProperties.grpc.blockchainPollingDelay * 5, TimeUnit.MILLISECONDS)
                    .until { true }
            }
            Mockito.verifyNoInteractions(mockedUserService)
        }
    }
}
