package com.ampnet.walletservice.controller

import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.TestBase
import com.ampnet.walletservice.config.DatabaseCleanerService
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.ErrorResponse
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.persistence.model.File
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.DocumentRepository
import com.ampnet.walletservice.persistence.repository.PairWalletCodeRepository
import com.ampnet.walletservice.persistence.repository.TransactionInfoRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.CloudStorageService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.util.UUID
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

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
abstract class ControllerTestBase : TestBase() {

    protected val userUuid: UUID = UUID.fromString("89fb3b1c-9c0a-11e9-a2a3-2a2ae2dbcce4")
    protected val organizationUuid: UUID = UUID.randomUUID()
    protected val projectUuid: UUID = UUID.randomUUID()
    protected val walletHash = "th_K3LCJLUQ1m2EsYmcNafGnRyEdDDgfPDGfZhmZ1YgbvAG35PQu"
    protected val signedTransaction = "tx_+RFNCwH4QrhARSL55I0DqhQePPV3J4ycxHpA9OkqnncvEJrYOThmo2h...signed-tx..."

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
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcRestDocumentation.document(
                "{ClassName}/{methodName}",
                Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                Preprocessors.preprocessResponse(Preprocessors.prettyPrint())))
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

    protected fun createWalletForUser(userUuid: UUID, hash: String) = createWallet(userUuid, hash, WalletType.USER)

    protected fun createWalletForProject(project: UUID, address: String) =
        createWallet(project, address, WalletType.PROJECT)

    protected fun createWalletForOrganization(organization: UUID, hash: String) =
        createWallet(organization, hash, WalletType.ORG)

    private fun createWallet(owner: UUID, hash: String, type: WalletType): Wallet {
        val wallet = Wallet(UUID.randomUUID(), owner, hash, type, Currency.EUR,
            ZonedDateTime.now(), hash, ZonedDateTime.now())
        return walletRepository.save(wallet)
    }

    protected fun saveFile(
        name: String,
        link: String,
        type: String,
        size: Int,
        createdByUserUuid: UUID
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

    protected fun getWalletHash(owner: UUID): String {
        val optionalUserWallet = walletRepository.findByOwner(owner)
        assertThat(optionalUserWallet).isPresent
        return getWalletHash(optionalUserWallet.get())
    }

    protected fun getWalletHash(wallet: Wallet): String =
        wallet.hash ?: fail("Wallet hash must be present")

    protected fun createUserResponse(
        uuid: UUID,
        email: String = "email@mail.com",
        first: String = "First",
        last: String = "Last",
        enabled: Boolean = true
    ): UserResponse = UserResponse.newBuilder()
        .setUuid(uuid.toString())
        .setEmail(email)
        .setFirstName(first)
        .setLastName(last)
        .setEnabled(enabled)
        .build()

    protected fun createApprovedDeposit(
        user: UUID,
        txHash: String? = null,
        amount: Long = 1000
    ): Deposit {
        val document = saveFile("doc", "document-link", "type", 1, user)
        val deposit = Deposit(0, user, "S34SDGFT", true, amount,
            user, ZonedDateTime.now(), document, txHash, ZonedDateTime.now())
        return depositRepository.save(deposit)
    }

    protected fun createApprovedWithdraw(
        owner: UUID,
        amount: Long = 1000,
        type: WalletType = WalletType.USER
    ): Withdraw {
        val withdraw = Withdraw(0, owner, amount, ZonedDateTime.now(), owner, "bank-account",
            "approved-tx", ZonedDateTime.now(),
            null, null, null, null, type)
        return withdrawRepository.save(withdraw)
    }

    protected fun createWithdraw(owner: UUID, amount: Long = 1000, type: WalletType = WalletType.USER): Withdraw {
        val withdraw = Withdraw(0, owner, amount, ZonedDateTime.now(), owner, "bank-account",
            null, null, null, null, null, null, type)
        return withdrawRepository.save(withdraw)
    }
}
