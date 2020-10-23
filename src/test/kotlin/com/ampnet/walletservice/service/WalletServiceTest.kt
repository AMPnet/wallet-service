package com.ampnet.walletservice.service

import com.ampnet.walletservice.controller.COOP
import com.ampnet.walletservice.controller.pojo.request.WalletCreateRequest
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.PairWalletCode
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.service.impl.TransactionInfoServiceImpl
import com.ampnet.walletservice.service.impl.WalletServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime
import java.util.UUID
import com.ampnet.mailservice.proto.WalletType as WalletTypeProto

class WalletServiceTest : JpaServiceTestBase() {

    private val walletService: WalletService by lazy {
        val transactionService = TransactionInfoServiceImpl(transactionInfoRepository)
        WalletServiceImpl(
            walletRepository, pairWalletCodeRepository,
            mockedBlockchainService, transactionService,
            mockedProjectService, mockedMailService, applicationProperties
        )
    }
    private lateinit var testContext: TestContext

    private val defaultAddressHash = "th_4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
    private val defaultPublicKey = "th_C2D7CF95645D33006175B78989035C7c9061d3F9"

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetWalletForUserId() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash, providerId)
        }

        verify("Service must fetch wallet for user with id") {
            val wallet = walletService.getWallet(userUuid) ?: fail("User must have a wallet")
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
            assertThat(wallet.providerId).isEqualTo(providerId)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForUser() {
        suppose("Wallet has pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
            val pairWalletCode = PairWalletCode(0, defaultPublicKey, "000000", ZonedDateTime.now())
            pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("Service can create wallet for a user") {
            val request = WalletCreateRequest(defaultPublicKey, "email", "provider_id")
            val user = createUserPrincipal(userUuid)
            val wallet = walletService.createUserWallet(user, request)
            assertThat(wallet.activationData).isEqualTo(request.publicKey)
            assertThat(wallet.email).isEqualTo(request.email)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(wallet.hash).isNull()
            assertThat(wallet.activatedAt).isNull()
            assertThat(wallet.coop).isEqualTo(user.coop)
            assertThat(wallet.providerId).isEqualTo(request.providerId)
        }
        verify("Wallet is assigned to the user") {
            val wallet = walletService.getWallet(userUuid) ?: fail("User must have a wallet")
            assertThat(wallet.activationData).isEqualTo(defaultPublicKey)
        }
        verify("Pair wallet code is deleted") {
            val optionalPairWalletCode = pairWalletCodeRepository.findByPublicKey(defaultPublicKey)
            assertThat(optionalPairWalletCode).isNotPresent
        }
        verify("Mail notification for created wallet") {
            Mockito.verify(mockedMailService, Mockito.times(1)).sendNewWalletMail(WalletTypeProto.USER, COOP)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForProject() {
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(signedTransaction)
            ).thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for project") {
            val wallet = walletService.createProjectWallet(projectUuid, signedTransaction, COOP)
            assertThat(wallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.PROJECT)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(wallet.hash).isNull()
            assertThat(wallet.activatedAt).isNull()
            assertThat(wallet.coop).isEqualTo(COOP)
            assertThat(wallet.providerId).isNull()
        }
        verify("Wallet is assigned to the project") {
            val projectWallet = walletService.getWallet(projectUuid) ?: fail("Missing project wallet")
            assertThat(projectWallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(projectWallet.hash).isNull()
            assertThat(projectWallet.providerId).isNull()
        }
        verify("Mail notification for created wallet") {
            Mockito.verify(mockedMailService, Mockito.times(1)).sendNewWalletMail(WalletTypeProto.PROJECT, COOP)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneUser() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }

        verify("Service cannot create additional account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createUserWallet(
                    createUserPrincipal(userUuid),
                    WalletCreateRequest(
                        defaultPublicKey, "alias", "provider_id"
                    )
                )
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneProject() {
        suppose("Project has a wallet") {
            createWalletForProject(projectUuid, defaultAddressHash)
        }

        verify("Service cannot create additional account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createProjectWallet(projectUuid, signedTransaction, COOP)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustBeAbleToGetWalletBalance() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("User has some funds on a wallet") {
            testContext.balance = 100
            Mockito.`when`(mockedBlockchainService.getBalance(defaultAddressHash)).thenReturn(testContext.balance)
        }

        verify("Service can return wallet balance") {
            val wallet = walletService.getWallet(userUuid) ?: fail("User must have a wallet")
            val balance = walletService.getWalletBalance(wallet)
            assertThat(balance).isEqualTo(testContext.balance)
        }
    }

    @Test
    fun mustThrowExceptionIfUserWithoutWalletTriesToGenerateCreateProjectWallet() {
        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(projectUuid, createUserPrincipal(userUuid))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationWithoutWalletTriesToGenerateCreateProjectWallet() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Project service will return project response") {
            Mockito.`when`(
                mockedProjectService.getProject(projectUuid)
            ).thenReturn(getProjectResponse(projectUuid, userUuid, organizationUuid))
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(projectUuid, createUserPrincipal(userUuid))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionWhenGenerateTransactionToCreateOrganizationWalletWithoutUserWallet() {
        verify("Service can generate create organization transaction") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateOrganizationWallet(organizationUuid, createUserPrincipal(userUuid))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationAlreadyHasWallet() {
        suppose("Organization has a wallet") {
            createWalletForOrganization(organizationUuid, defaultAddressHash)
        }

        verify("Service will throw exception that organization already has a wallet") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.generateTransactionToCreateOrganizationWallet(organizationUuid, createUserPrincipal(userUuid))
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustGenerateTransactionToCreateOrganizationWallet() {
        suppose("User has a wallet") {
            testContext.wallet = createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Blockchain service will generate transaction") {
            Mockito.`when`(
                mockedBlockchainService.generateCreateOrganizationTransaction(getWalletHash(testContext.wallet.owner))
            ).thenReturn(transactionData)
        }
        suppose("Project service will return organization response") {
            Mockito.`when`(
                mockedProjectService.getOrganization(organizationUuid)
            ).thenReturn(getOrganizationResponse(organizationUuid, userUuid))
        }

        verify("Service can generate transaction") {
            val transaction = walletService
                .generateTransactionToCreateOrganizationWallet(organizationUuid, createUserPrincipal(userUuid))
            assertThat(transaction.transactionData).isEqualTo(transactionData)
        }
    }

    @Test
    fun mustBeAbleToCreateOrganizationWallet() {
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(signedTransaction)
            ).thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for organization") {
            val wallet = walletService.createOrganizationWallet(organizationUuid, signedTransaction, COOP)
            assertThat(wallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.ORG)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(wallet.hash).isNull()
            assertThat(wallet.activatedAt).isNull()
            assertThat(wallet.coop).isEqualTo(COOP)
            assertThat(wallet.providerId).isNull()
        }
        verify("Wallet is assigned to the organization") {
            val wallet = walletService.getWallet(organizationUuid) ?: fail("Missing organization wallet")
            assertThat(wallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(wallet.hash).isNull()
            assertThat(wallet.providerId).isNull()
        }
        verify("Mail notification for created wallet") {
            Mockito.verify(mockedMailService, Mockito.times(1)).sendNewWalletMail(WalletTypeProto.ORGANIZATION, COOP)
        }
    }

    @Test
    fun mustThrowExceptionForCreateOrganizationWalletIfOrganizationAlreadyHasWallet() {
        suppose("Organization has a wallet") {
            createWalletForOrganization(organizationUuid, defaultAddressHash)
        }

        verify("Service cannot create additional organization account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createOrganizationWallet(organizationUuid, signedTransaction, COOP)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustNotBeAbleToCreateWalletWithTheSameHashInsideCoop() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Blockchain service will return same hash for new project wallet transaction") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(signedTransaction)
            ).thenReturn(defaultAddressHash)
        }

        verify("User will not be able to create organization wallet with the same hash") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createProjectWallet(projectUuid, signedTransaction, COOP)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_HASH_EXISTS)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletWithTheSameHashInAnotherCoop() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultPublicKey)
        }

        verify("New user has the same public key") {
            val newCoop = "new-coop"
            val request = WalletCreateRequest(defaultPublicKey, "email", "provider_id")
            val wallet = walletService.createUserWallet(createUserPrincipal(UUID.randomUUID(), coop = newCoop), request)
            assertThat(wallet.activationData).isEqualTo(defaultPublicKey)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(wallet.hash).isNull()
            assertThat(wallet.activatedAt).isNull()
            assertThat(wallet.coop).isEqualTo(newCoop)
        }
    }

    @Test
    fun mustGenerateNewPairWalletCodeForExistingAddress() {
        suppose("Pair wallet code exists") {
            databaseCleanerService.deleteAllPairWalletCodes()
            val pairWalletCode = PairWalletCode(0, "adr_423242", "SD432X", ZonedDateTime.now())
            testContext.pairWalletCode = pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("Service will create new pair wallet code") {
            val newPairWalletCode = walletService.generatePairWalletCode(testContext.pairWalletCode.publicKey)
            assertThat(newPairWalletCode.publicKey).isEqualTo(testContext.pairWalletCode.publicKey)
        }
        verify("Old pair wallet code is deleted") {
            val oldPairWalletCode = pairWalletCodeRepository.findById(testContext.pairWalletCode.id)
            assertThat(oldPairWalletCode).isNotPresent
        }
    }

    private class TestContext {
        lateinit var wallet: Wallet
        var balance: Long = -1
        lateinit var pairWalletCode: PairWalletCode
    }
}
