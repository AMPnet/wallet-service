package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.CooperativeWalletService
import com.ampnet.walletservice.service.TransactionInfoService
import com.ampnet.walletservice.service.pojo.OrganizationWithWallet
import com.ampnet.walletservice.service.pojo.ProjectWithWallet
import com.ampnet.walletservice.service.pojo.UserWithWallet
import com.ampnet.walletservice.grpc.userservice.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class CooperativeWalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val userService: UserService,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val projectService: ProjectService
) : CooperativeWalletService {

    @Transactional
    override fun generateWalletActivationTransaction(walletUuid: UUID, userUuid: UUID): TransactionDataAndInfo {
        val wallet = getWalletByUuid(walletUuid)
        val data = blockchainService.addWallet(wallet.activationData)
        val info = transactionInfoService.activateWalletTransaction(wallet.uuid, wallet.type, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun activateWallet(walletUuid: UUID, signedTransaction: String): Wallet {
        val wallet = getWalletByUuid(walletUuid)
        wallet.hash = blockchainService.postTransaction(signedTransaction)
        wallet.activatedAt = ZonedDateTime.now()
        return walletRepository.save(wallet)
    }

    @Transactional(readOnly = true)
    override fun getAllUserWithUnactivatedWallet(): List<UserWithWallet> {
        val wallets = walletRepository.findUnactivatedByType(WalletType.USER).associateBy { it.owner }
        val users = userService.getUsers(wallets.keys)
        return users.mapNotNull { user ->
            wallets[UUID.fromString(user.uuid)]?.let { wallet ->
                UserWithWallet(user, wallet)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun getOrganizationsWithUnactivatedWallet(): List<OrganizationWithWallet> {
        val wallets = walletRepository.findUnactivatedByType(WalletType.ORG).associateBy { it.owner }
        val organizations = projectService.getOrganizations(wallets.keys)
        return organizations.mapNotNull { organization ->
            wallets[UUID.fromString(organization.uuid)]?.let { wallet ->
                OrganizationWithWallet(organization, wallet)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun getProjectsWithUnactivatedWallet(): List<ProjectWithWallet> {
        val wallets = walletRepository.findUnactivatedByType(WalletType.PROJECT).associateBy { it.owner }
        val projects = projectService.getProjects(wallets.keys)
        return projects.mapNotNull { project ->
            wallets[UUID.fromString(project.uuid)]?.let { wallet ->
                ProjectWithWallet(project, wallet)
            }
        }
    }

    private fun getWalletByUuid(walletUuid: UUID): Wallet = walletRepository.findById(walletUuid).orElseThrow {
        throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Missing wallet uuid: $walletUuid")
    }
}
