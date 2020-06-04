package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.SellOfferService
import com.ampnet.walletservice.service.pojo.ProjectWithSellOffers
import java.util.UUID
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class SellOfferServiceImpl(
    private val projectService: ProjectService,
    private val blockchainService: BlockchainService,
    private val walletRepository: WalletRepository
) : SellOfferService {

    companion object : KLogging()

    override fun getProjectsWithSalesOffers(): List<ProjectWithSellOffers> {
        val activeOffers = blockchainService.getSellOffers()
        val distinctProjectWalletHashes = activeOffers.map { it.projectWalletHash }.toSet()
        val projectWallets = walletRepository.findAllByActivationData(distinctProjectWalletHashes)
            .toList().associateBy { it.owner }
        val projects = projectService.getProjects(projectWallets.keys)

        return projects.mapNotNull { project ->
            val projectUuid = UUID.fromString(project.uuid)
            projectWallets[projectUuid]?.let { wallet ->
                val offersForProject = activeOffers.filter { it.projectWalletHash == wallet.hash }
                ProjectWithSellOffers(project, offersForProject)
            }
        }
    }
}
