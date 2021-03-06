package com.ampnet.walletservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.walletservice.amqp.mailservice.MailService
import com.ampnet.walletservice.amqp.mailservice.WalletTypeAmqp
import com.ampnet.walletservice.controller.pojo.request.WalletTransferRequest
import com.ampnet.walletservice.enums.TransferWalletType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.GrpcHandledException
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.CooperativeWalletService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.response.OrganizationWithWallet
import com.ampnet.walletservice.service.pojo.response.ProjectWithWallet
import com.ampnet.walletservice.service.pojo.response.UserWithWallet
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class CooperativeWalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val userService: UserService,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val projectService: ProjectService,
    private val mailService: MailService
) : CooperativeWalletService {

    companion object : KLogging()

    @Transactional
    @Throws(ResourceNotFoundException::class, GrpcException::class, GrpcHandledException::class)
    override fun generateWalletActivationTransaction(walletUuid: UUID, user: UserPrincipal): TransactionDataAndInfo {
        val wallet = getWalletByUuid(walletUuid)
        val data = blockchainService.addWallet(wallet.activationData, wallet.coop)
        val info = transactionInfoService.activateWalletTransaction(wallet.uuid, wallet.type, user)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, GrpcException::class, GrpcHandledException::class)
    override fun activateWallet(walletUuid: UUID, signedTransaction: String): Wallet {
        val wallet = getWalletByUuid(walletUuid)
        wallet.hash = blockchainService.postTransaction(signedTransaction, wallet.coop)
        wallet.activatedAt = ZonedDateTime.now()
        mailService.sendWalletActivated(getWalletType(wallet.type), wallet.owner, wallet.activationData)
        return wallet
    }

    @Transactional(readOnly = true)
    override fun getAllUserWithUnactivatedWallet(coop: String, pageable: Pageable): Page<UserWithWallet> {
        val walletsPage = walletRepository.findUnactivatedByType(WalletType.USER, coop, pageable)
        val wallets = walletsPage.toList().associateBy { it.owner }
        val users = userService.getUsers(wallets.keys)
        val usersWithWallet = users.mapNotNull { user ->
            wallets[user.uuid]?.let { wallet ->
                UserWithWallet(user, wallet)
            }
        }
        return PageImpl(usersWithWallet, pageable, walletsPage.totalElements)
    }

    @Transactional(readOnly = true)
    override fun getOrganizationsWithUnactivatedWallet(coop: String, pageable: Pageable): Page<OrganizationWithWallet> {
        val walletsPage = walletRepository.findUnactivatedByType(WalletType.ORG, coop, pageable)
        val wallets = walletsPage.toList().associateBy { it.owner }
        val organizations = projectService.getOrganizations(wallets.keys)
        val organizationsWithWallet = organizations.mapNotNull { organization ->
            wallets[UUID.fromString(organization.uuid)]?.let { wallet ->
                OrganizationWithWallet(organization, wallet)
            }
        }
        return PageImpl(organizationsWithWallet, pageable, walletsPage.totalElements)
    }

    @Transactional(readOnly = true)
    override fun getProjectsWithUnactivatedWallet(coop: String, pageable: Pageable): Page<ProjectWithWallet> {
        val walletsPage = walletRepository.findUnactivatedByType(WalletType.PROJECT, coop, pageable)
        val wallets = walletsPage.toList().associateBy { it.owner }
        val projects = projectService.getProjects(wallets.keys)
        val projectsWithWallet = projects.mapNotNull { project ->
            wallets[project.uuid]?.let { wallet ->
                ProjectWithWallet(project, wallet)
            }
        }
        return PageImpl(projectsWithWallet, pageable, walletsPage.totalElements)
    }

    @Transactional
    @Throws(
        ResourceNotFoundException::class, InvalidRequestException::class,
        GrpcException::class, GrpcHandledException::class
    )
    override fun generateSetTransferOwnership(
        owner: UserPrincipal,
        request: WalletTransferRequest
    ): TransactionDataAndInfo {
        val userWallet = ServiceUtils.getWalletByUserUuid(request.userUuid, walletRepository)
        if (userWallet.hash == null) {
            throw InvalidRequestException(
                ErrorCode.WALLET_NOT_ACTIVATED, "Cannot transfer ownership to unactivated wallet"
            )
        }
        val data = when (request.type) {
            TransferWalletType.TOKEN_ISSUER ->
                blockchainService.generateTransferTokenIssuer(userWallet.activationData, userWallet.coop)
            TransferWalletType.PLATFORM_MANAGER ->
                blockchainService.generateTransferPlatformManager(userWallet.activationData, userWallet.coop)
        }
        val info = transactionInfoService.createTransferOwnership(owner, request)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional(readOnly = true)
    @Throws(GrpcException::class, GrpcHandledException::class)
    override fun transferOwnership(signedTransaction: String, coop: String): String =
        blockchainService.postTransaction(signedTransaction, coop)

    @Transactional
    @Throws(ResourceNotFoundException::class, InvalidRequestException::class)
    override fun activateAdminWallet(address: String, coop: String, hash: String): Wallet {
        val wallet = getWalletByAddress(address, coop)
        wallet.hash?.let {
            throw InvalidRequestException(ErrorCode.WALLET_HASH_EXISTS, "Wallet with hash: $it already activated")
        }
        wallet.hash = hash
        wallet.activatedAt = ZonedDateTime.now()
        return wallet
    }

    @Throws(GrpcException::class, GrpcHandledException::class, ResourceNotFoundException::class)
    override fun updateCoopUserRoles(coop: String) {
        logger.info { "Updating user roles for coop: $coop" }
        val platformManagerAddress = blockchainService.getPlatformManager(coop)
        val tokenIssuerAddress = blockchainService.getTokenIssuer(coop)
        if (platformManagerAddress == tokenIssuerAddress) {
            setUserRole(platformManagerAddress, SetRoleRequest.Role.ADMIN, coop)
            return
        }
        setUserRole(tokenIssuerAddress, SetRoleRequest.Role.TOKEN_ISSUER, coop)
        setUserRole(platformManagerAddress, SetRoleRequest.Role.PLATFORM_MANAGER, coop)
    }

    private fun setUserRole(walletAddress: String, role: SetRoleRequest.Role, coop: String) {
        val userWallet = getWalletByAddress(walletAddress, coop)
        userService.setUserRole(userWallet.owner, role, coop)
        logger.info { "Set new user role: $role to user: ${userWallet.owner}" }
    }

    private fun getWalletByUuid(walletUuid: UUID): Wallet = walletRepository.findById(walletUuid).orElseThrow {
        throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Missing wallet uuid: $walletUuid")
    }

    private fun getWalletByAddress(address: String, coop: String): Wallet =
        walletRepository.findByActivationDataAndCoop(address, coop).orElseThrow {
            throw ResourceNotFoundException(
                ErrorCode.WALLET_MISSING, "Missing wallet with address: $address in coop: $coop"
            )
        }

    private fun getWalletType(type: WalletType): WalletTypeAmqp {
        return when (type) {
            WalletType.USER -> WalletTypeAmqp.USER
            WalletType.PROJECT -> WalletTypeAmqp.PROJECT
            WalletType.ORG -> WalletTypeAmqp.ORGANIZATION
        }
    }
}
