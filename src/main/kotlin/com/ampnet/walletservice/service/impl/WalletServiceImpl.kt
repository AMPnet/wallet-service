package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.config.ApplicationProperties
import com.ampnet.walletservice.controller.pojo.request.WalletCreateRequest
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceAlreadyExistsException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.PairWalletCode
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.PairWalletCodeRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.WalletService
import com.ampnet.walletservice.service.pojo.response.ProjectWithWallet
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID
import com.ampnet.mailservice.proto.WalletType as WalletTypeProto

@Service
@Suppress("TooManyFunctions")
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val pairWalletCodeRepository: PairWalletCodeRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val projectService: ProjectService,
    private val mailService: MailService,
    private val applicationProperties: ApplicationProperties
) : WalletService {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        private const val PAIR_WALLET_CODE_LENGTH = 6
    }

    @Transactional(readOnly = true)
    @Throws(GrpcException::class)
    override fun getWalletBalance(wallet: Wallet): Long? {
        val walletHash = wallet.hash ?: return null
        return blockchainService.getBalance(walletHash)
    }

    @Transactional(readOnly = true)
    override fun getWallet(owner: UUID): Wallet? {
        return ServiceUtils.wrapOptional(walletRepository.findByOwner(owner))
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createUserWallet(user: UserPrincipal, request: WalletCreateRequest): Wallet {
        walletRepository.findByOwner(user.uuid).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS, "User: ${user.uuid} already has a wallet.")
        }
        pairWalletCodeRepository.findByPublicKey(request.publicKey).ifPresent {
            pairWalletCodeRepository.delete(it)
        }

        logger.debug { "Creating wallet: $request for user: ${user.uuid}" }
        val wallet = createWallet(
            user.uuid, request.publicKey, WalletType.USER,
            user.coop, request.email, request.providerId
        )
        mailService.sendNewWalletMail(WalletTypeProto.USER, user.coop)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateProjectWallet(project: UUID, user: UserPrincipal): TransactionDataAndInfo {
        throwExceptionIfProjectHasWallet(project)
        val userWalletHash = ServiceUtils.getWalletHash(user.uuid, walletRepository)

        logger.debug { "Generating create wallet transaction for project: $project" }
        val projectResponse = projectService.getProject(project)
        ServiceUtils.validateUserIsProjectOwner(user.uuid, projectResponse)

        val organizationWalletHash = ServiceUtils.getWalletHash(projectResponse.organizationUuid, walletRepository)
        val request = GenerateProjectWalletRequest(
            userWalletHash,
            organizationWalletHash,
            projectResponse
        )
        val data = blockchainService.generateProjectWalletTransaction(request)
        val info = transactionInfoService.createProjectTransaction(project, projectResponse.name, user)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createProjectWallet(project: UUID, signedTransaction: String, coop: String): Wallet {
        throwExceptionIfProjectHasWallet(project)
        logger.debug { "Creating wallet for project: $project" }
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(project, txHash, WalletType.PROJECT, coop)
        logger.debug { "Created wallet for project: $project" }
        mailService.sendNewWalletMail(WalletTypeProto.PROJECT, coop)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateOrganizationWallet(
        organization: UUID,
        user: UserPrincipal
    ): TransactionDataAndInfo {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val userWalletHash = ServiceUtils.getWalletHash(user.uuid, walletRepository)
        logger.debug { "Generating create wallet transaction for organization: $organization" }
        val organizationResponse = projectService.getOrganization(organization)
        if (organizationResponse.createdByUser != user.uuid.toString()) {
            throw InvalidRequestException(
                ErrorCode.ORG_MISSING_PRIVILEGE,
                "User: $user did not create this organization: $organization and cannot create a wallet"
            )
        }
        val data = blockchainService.generateCreateOrganizationTransaction(userWalletHash)
        val info =
            transactionInfoService.createOrgTransaction(organization, organizationResponse.name, user)
        logger.debug { "Generated create wallet transaction for organization: $organization" }
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun createOrganizationWallet(organization: UUID, signedTransaction: String, coop: String): Wallet {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        logger.debug { "Creating wallet for organization: $organization" }
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(organization, txHash, WalletType.ORG, coop)
        logger.debug { "Created wallet for organization: $organization" }
        mailService.sendNewWalletMail(WalletTypeProto.ORGANIZATION, coop)
        return wallet
    }

    @Transactional
    override fun generatePairWalletCode(publicKey: String): PairWalletCode {
        pairWalletCodeRepository.findByPublicKey(publicKey).ifPresent {
            pairWalletCodeRepository.delete(it)
        }
        val code = generatePairWalletCode()
        val pairWalletCode = PairWalletCode(0, publicKey, code, ZonedDateTime.now())
        return pairWalletCodeRepository.save(pairWalletCode)
    }

    @Transactional(readOnly = true)
    override fun getPairWalletCode(code: String): PairWalletCode? {
        return ServiceUtils.wrapOptional(pairWalletCodeRepository.findByCode(code))
    }

    @Transactional(readOnly = true)
    override fun getProjectsWithActiveWallet(coop: String?, pageable: Pageable): Page<ProjectWithWallet> {
        val walletsPage = walletRepository.findActivatedByType(
            WalletType.PROJECT, coop ?: applicationProperties.coop.default, pageable
        )
        val projectWallets = walletsPage.toList()
            .filter { it.hash != null }
            .associateBy { it.owner }
        if (projectWallets.isEmpty()) {
            return PageImpl(emptyList(), pageable, walletsPage.totalElements)
        }

        val now = ZonedDateTime.now()
        val projectWalletHashes = projectWallets.values.mapNotNull { it.hash }
        val projectsInfo = blockchainService.getProjectsInfo(projectWalletHashes)
            .toList()
            .associateBy { it.txHash }
        val projectsWithWallet = projectService.getProjects(projectWallets.keys)
            .filter { it.active && it.endDate.isAfter(now) }
            .mapNotNull { project ->
                projectWallets[project.uuid]?.let { wallet ->
                    val projectInfo = projectsInfo[wallet.hash]
                    val balance = projectInfo?.balance
                    val payoutInProcess = projectInfo?.payoutInProcess
                    ProjectWithWallet(project, wallet, balance, payoutInProcess)
                }
            }
        return PageImpl(projectsWithWallet, pageable, walletsPage.totalElements)
    }

    private fun createWallet(
        owner: UUID,
        activationData: String,
        type: WalletType,
        coop: String,
        email: String? = null,
        providerId: String? = null
    ): Wallet {
        if (walletRepository.findByActivationDataAndCoop(activationData, coop).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.WALLET_HASH_EXISTS,
                "Trying to create wallet: $type with existing activationData: $activationData"
            )
        }
        val wallet = Wallet(owner, activationData, type, Currency.EUR, coop, email, providerId)
        return walletRepository.save(wallet)
    }

    private fun throwExceptionIfProjectHasWallet(project: UUID) {
        if (walletRepository.findByOwner(project).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS, "Project: $project already has a wallet.")
        }
    }

    private fun throwExceptionIfOrganizationAlreadyHasWallet(organization: UUID) {
        if (walletRepository.findByOwner(organization).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.WALLET_EXISTS,
                "Organization: $organization already has a wallet."
            )
        }
    }

    private fun generatePairWalletCode(): String = (1..PAIR_WALLET_CODE_LENGTH)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
