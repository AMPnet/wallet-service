package com.ampnet.walletservice.service

import com.ampnet.walletservice.TestBase
import com.ampnet.walletservice.config.DatabaseCleanerService
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionData
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.mail.MailServiceImpl
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.projectservice.ProjectServiceImpl
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.model.File
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.BankAccountRepository
import com.ampnet.walletservice.persistence.repository.DeclinedRepository
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.DocumentRepository
import com.ampnet.walletservice.persistence.repository.PairWalletCodeRepository
import com.ampnet.walletservice.persistence.repository.TransactionInfoRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.impl.CloudStorageServiceImpl
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Transactional(propagation = Propagation.SUPPORTS)
@Import(DatabaseCleanerService::class)
abstract class JpaServiceTestBase : TestBase() {

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    protected lateinit var walletRepository: WalletRepository

    @Autowired
    protected lateinit var documentRepository: DocumentRepository

    @Autowired
    protected lateinit var transactionInfoRepository: TransactionInfoRepository

    @Autowired
    protected lateinit var pairWalletCodeRepository: PairWalletCodeRepository

    @Autowired
    protected lateinit var withdrawRepository: WithdrawRepository

    @Autowired
    protected lateinit var depositRepository: DepositRepository

    @Autowired
    protected lateinit var declinedRepository: DeclinedRepository

    @Autowired
    protected lateinit var bankAccountRepository: BankAccountRepository

    protected val mockedBlockchainService: BlockchainService = Mockito.mock(BlockchainService::class.java)
    protected val mockedCloudStorageService: CloudStorageServiceImpl = Mockito.mock(CloudStorageServiceImpl::class.java)
    protected val mockedMailService: MailService = Mockito.mock(MailServiceImpl::class.java)
    protected val mockedProjectService: ProjectService = Mockito.mock(ProjectServiceImpl::class.java)
    protected val userUuid: UUID = UUID.randomUUID()
    protected val organizationUuid: UUID = UUID.randomUUID()
    protected val projectUuid: UUID = UUID.randomUUID()
    protected val signedTransaction = "signed-transaction"
    protected val txHash = "tx-hash"
    protected val transactionData = TransactionData("data")
    protected val bankAccount = "AL35202111090000000001234567"
    protected val providerId = "provider_id"

    protected fun createWalletForUser(userUuid: UUID, hash: String, providerId: String? = null) =
        createWallet(userUuid, hash, WalletType.USER, providerId)

    protected fun createWalletForProject(project: UUID, hash: String) =
        createWallet(project, hash, WalletType.PROJECT)

    protected fun createWalletForOrganization(organization: UUID, hash: String) =
        createWallet(organization, hash, WalletType.ORG)

    protected fun createWallet(owner: UUID, hash: String, type: WalletType, providerId: String? = null): Wallet {
        val wallet = Wallet(
            UUID.randomUUID(), owner, hash, type, Currency.EUR,
            ZonedDateTime.now(), hash, ZonedDateTime.now(), null, providerId
        )
        return walletRepository.save(wallet)
    }

    protected fun saveFile(createdByUserUuid: UUID): File {
        val document = File(0, "doc-link", "doc", "document/type", 100, createdByUserUuid, ZonedDateTime.now())
        return documentRepository.save(document)
    }

    protected fun getWalletHash(owner: UUID): String {
        val wallet = walletRepository.findByOwner(owner)
        if (wallet.isPresent) {
            return wallet.get().hash ?: fail("Wallet must be activated")
        }
        fail("Missing wallet")
    }

    protected fun createBurnedWithdraw(user: UUID, type: DepositWithdrawType = DepositWithdrawType.USER): Withdraw {
        val withdraw = Withdraw(
            0, user, 100L, ZonedDateTime.now(), user, bankAccount,
            "approved-tx", ZonedDateTime.now(),
            "burned-tx", ZonedDateTime.now(), UUID.randomUUID(), null, type
        )
        return withdrawRepository.save(withdraw)
    }

    protected fun createApprovedWithdraw(user: UUID, type: DepositWithdrawType = DepositWithdrawType.USER): Withdraw {
        val withdraw = Withdraw(
            0, user, 100L, ZonedDateTime.now(), user, bankAccount,
            "approved-tx", ZonedDateTime.now(),
            null, null, null, null, type
        )
        return withdrawRepository.save(withdraw)
    }

    protected fun createWithdraw(user: UUID, type: DepositWithdrawType = DepositWithdrawType.USER): Withdraw {
        val withdraw = Withdraw(
            0, user, 100L, ZonedDateTime.now(), userUuid, bankAccount,
            null, null, null, null, null, null, type
        )
        return withdrawRepository.save(withdraw)
    }

    protected fun createApprovedDeposit(
        txHash: String?,
        type: DepositWithdrawType = DepositWithdrawType.USER
    ): Deposit {
        val document = saveFile(userUuid)
        val deposit = Deposit(
            0, userUuid, "S34SDGFT", 10_000,
            ZonedDateTime.now(), userUuid, type, txHash, userUuid, ZonedDateTime.now(), document, null
        )
        return depositRepository.save(deposit)
    }

    protected fun createUnsigned(owner: UUID, type: DepositWithdrawType = DepositWithdrawType.USER): Deposit {
        val deposit = Deposit(
            0, owner, "S34SDGFT", 10_000,
            ZonedDateTime.now(), userUuid, type, null, null, null, null, null
        )
        return depositRepository.save(deposit)
    }
}
