package com.ampnet.walletservice.controller

import com.ampnet.projectservice.proto.OrganizationMembershipResponse
import com.ampnet.walletservice.TestBase
import com.ampnet.walletservice.config.DatabaseCleanerService
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.ErrorResponse
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.BankAccount
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.model.File
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.BankAccountRepository
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.DocumentRepository
import com.ampnet.walletservice.persistence.repository.PairWalletCodeRepository
import com.ampnet.walletservice.persistence.repository.RevenuePayoutRepository
import com.ampnet.walletservice.persistence.repository.TransactionInfoRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.CloudStorageService
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import com.ampnet.walletservice.service.pojo.response.UserServiceResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.ZonedDateTime
import java.util.UUID

const val COOP = "ampnet-test"

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
abstract class ControllerTestBase : TestBase() {

    protected val userUuid: UUID = UUID.fromString("89fb3b1c-9c0a-11e9-a2a3-2a2ae2dbcce4")
    protected val organizationUuid: UUID = UUID.randomUUID()
    protected val projectUuid: UUID = UUID.randomUUID()
    protected val walletHash = "th_K3LCJLUQ1m2EsYmcNafGnRyEdDDgfPDGfZhmZ1YgbvAG35PQu"
    protected val signedTransaction = "tx_+RFNCwH4QrhARSL55I0DqhQePPV3J4ycxHpA9OkqnncvEJrYOThmo2h...signed-tx..."
    protected val anotherCoop = "another coop"
    protected val txHash = "th_2cNtX3hdmGPHq8sgHb6Lcu87iEc3E6feHTWczQAViQjmP7evbP"
    protected val bankCode = "DABAIE2D"

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    protected lateinit var walletRepository: WalletRepository

    @Autowired
    protected lateinit var transactionInfoRepository: TransactionInfoRepository

    @Autowired
    protected lateinit var pairWalletCodeRepository: PairWalletCodeRepository

    @Autowired
    protected lateinit var depositRepository: DepositRepository

    @Autowired
    protected lateinit var withdrawRepository: WithdrawRepository

    @Autowired
    private lateinit var documentRepository: DocumentRepository

    @Autowired
    protected lateinit var revenuePayoutRepository: RevenuePayoutRepository

    @Autowired
    private lateinit var bankAccountRepository: BankAccountRepository

    @MockBean
    protected lateinit var userService: UserService

    @MockBean
    protected lateinit var mailService: MailService

    @MockBean
    protected lateinit var projectService: ProjectService

    @MockBean
    protected lateinit var blockchainService: BlockchainService

    @MockBean
    protected lateinit var cloudStorageService: CloudStorageService

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun init(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(
                MockMvcRestDocumentation.document(
                    "{ClassName}/{methodName}",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                )
            )
            .build()
    }

    protected fun getResponseErrorCode(errorCode: ErrorCode): String {
        return errorCode.categoryCode + errorCode.specificCode
    }

    protected fun verifyResponseErrorCode(result: MvcResult, errorCode: ErrorCode) {
        val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
        val expectedErrorCode = getResponseErrorCode(errorCode)
        assert(response.errCode == expectedErrorCode)
    }

    protected fun createWalletForUser(userUuid: UUID, hash: String, coop: String = COOP, providerId: String? = null) =
        createWallet(userUuid, hash, WalletType.USER, coop, providerId)

    protected fun createWalletForProject(project: UUID, address: String, coop: String = COOP) =
        createWallet(project, address, WalletType.PROJECT, coop)

    protected fun createWalletForOrganization(organization: UUID, hash: String, coop: String = COOP) =
        createWallet(organization, hash, WalletType.ORG, coop)

    private fun createWallet(
        owner: UUID,
        hash: String,
        type: WalletType,
        coop: String,
        providerId: String? = null
    ): Wallet {
        val email = if (type == WalletType.USER) {
            "wallet_email"
        } else {
            null
        }
        val wallet = Wallet(
            UUID.randomUUID(), owner, hash, type, Currency.EUR,
            ZonedDateTime.now(), hash, ZonedDateTime.now(), coop, email, providerId
        )
        return walletRepository.save(wallet)
    }

    protected fun saveFile(
        name: String = "name",
        link: String = "link",
        type: String = "type",
        size: Int = 1000,
        createdByUserUuid: UUID = userUuid
    ): File {
        val document = File::class.java.getDeclaredConstructor().newInstance()
        document.name = name
        document.link = link
        document.type = type
        document.size = size
        document.createdByUserUuid = createdByUserUuid
        document.createdAt = ZonedDateTime.now()
        return documentRepository.save(document)
    }

    protected fun getWallet(owner: UUID): Wallet {
        val optionalUserWallet = walletRepository.findByOwner(owner)
        assertThat(optionalUserWallet).isPresent
        return optionalUserWallet.get()
    }

    protected fun getWalletHash(owner: UUID): String = getWalletHash(getWallet(owner))

    protected fun getWalletHash(wallet: Wallet): String =
        wallet.hash ?: fail("Wallet hash must be present")

    protected fun createUserResponse(
        uuid: UUID,
        email: String = "email@mail.com",
        first: String = "First",
        last: String = "Last",
        enabled: Boolean = true
    ): UserServiceResponse = UserServiceResponse(uuid, email, first, last, enabled)

    protected fun createApprovedDeposit(
        owner: UUID,
        amount: Long = 1000,
        type: DepositWithdrawType = DepositWithdrawType.USER,
        txHash: String = "th_ktDw9ytaQ9aSi78qgCAw2JhdzS8F7vGgzYvWeMdRtP6hJnQqG",
        coop: String = COOP
    ): Deposit {
        val document = saveFile("doc", "document-link", "type", 1, owner)
        val deposit = Deposit(
            0, owner, "S34SDGFT", amount,
            ZonedDateTime.now(), userUuid, type, txHash, userUuid,
            ZonedDateTime.now(), document, coop
        )
        return depositRepository.save(deposit)
    }

    protected fun createApprovedWithdraw(
        owner: UUID,
        amount: Long = 1000,
        type: DepositWithdrawType = DepositWithdrawType.USER,
        txHash: String = "approved-tx",
        coop: String = COOP,
        withFile: Boolean = false
    ): Withdraw {
        val withdraw = Withdraw(
            0, owner, amount, ZonedDateTime.now(), userUuid, "bank-account",
            txHash, ZonedDateTime.now(), null, null, null, null, type, coop, bankCode
        )
        if (withFile) withdraw.file = saveFile("doc", "document-link", "type", 1, owner)
        return withdrawRepository.save(withdraw)
    }

    protected fun createWithdraw(
        owner: UUID,
        amount: Long = 1000,
        type: DepositWithdrawType = DepositWithdrawType.USER,
        userUuid: UUID? = null,
        coop: String = COOP
    ): Withdraw {
        val user = userUuid ?: owner
        val withdraw = Withdraw(
            0, owner, amount, ZonedDateTime.now(), user, "bank-account", null,
            null, null, null, null, null, type, coop, bankCode
        )
        return withdrawRepository.save(withdraw)
    }

    protected fun createUnsignedDeposit(
        owner: UUID,
        type: DepositWithdrawType = DepositWithdrawType.USER,
        withFile: Boolean = false,
        amount: Long = 0,
        coop: String = COOP
    ): Deposit {
        val file = if (withFile) saveFile() else null
        val approvedBy = if (withFile) userUuid else null
        val approvedAt = if (withFile) ZonedDateTime.now() else null
        val deposit = Deposit(
            0, owner, "S34SDGFT", amount,
            ZonedDateTime.now(), userUuid, type, null, approvedBy, approvedAt, file, coop
        )
        return depositRepository.save(deposit)
    }

    protected fun createProjectResponse(
        uuid: UUID,
        name: String = "project",
        createdByUser: UUID = UUID.randomUUID(),
        currency: String = "EUR",
        organizationUuid: UUID = UUID.randomUUID(),
        imageUrl: String = "image_url",
        description: String = "Description",
        expectedFunding: Long = 100000000L
    ): ProjectServiceResponse = ProjectServiceResponse(
        uuid, name, description,
        ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(100),
        expectedFunding, currency, 1L, expectedFunding, true, imageUrl, createdByUser, organizationUuid
    )

    protected fun createBankAccount(
        iban: String,
        bankCode: String,
        createdBy: UUID,
        alias: String,
        coop: String = COOP
    ): BankAccount {
        val bankAccount = BankAccount(iban, bankCode, createdBy, alias, coop)
        return bankAccountRepository.save(bankAccount)
    }

    protected fun createOrganizationMembership(userUuid: UUID): OrganizationMembershipResponse {
        return OrganizationMembershipResponse.newBuilder().setUserUuid(userUuid.toString()).build()
    }
}
