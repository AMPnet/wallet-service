package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.SellOfferService
import com.ampnet.walletservice.service.pojo.response.ProjectWithSellOffers
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SellOfferServiceImpl(
    private val projectService: ProjectService,
    private val blockchainService: BlockchainService,
    private val walletRepository: WalletRepository
) : SellOfferService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getProjectsWithSalesOffers(coop: String): List<ProjectWithSellOffers> {
        logger.debug { "Get all projects with sales offers" }
        val activeOffers = blockchainService.getSellOffers(coop)
        val distinctProjectWalletHashes = activeOffers.map { it.projectWalletHash }.toSet()
        logger.debug { "Distinct project wallet hashes: $distinctProjectWalletHashes" }
        val projectWallets = walletRepository.findByHashes(distinctProjectWalletHashes)
            .toList().associateBy { it.owner }
        logger.debug { "Distinct project wallet owners: ${projectWallets.keys}" }
        val projects = projectService.getProjects(projectWallets.keys)

        return projects.mapNotNull { project ->
            projectWallets[project.uuid]?.let { wallet ->
                val offersForProject = activeOffers.filter { it.projectWalletHash == wallet.hash }
                ProjectWithSellOffers(project, offersForProject)
            }
        }
    }
}
