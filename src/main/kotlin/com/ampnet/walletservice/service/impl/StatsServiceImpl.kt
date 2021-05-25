package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.service.StatsService
import com.ampnet.walletservice.service.pojo.response.StatsResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class StatsServiceImpl(
    private val depositRepository: DepositRepository,
    private val walletRepository: WalletRepository,
    private val blockchainService: BlockchainService
) : StatsService {

    override fun getStats(coop: String): StatsResponse {
        val initializedWallets = walletRepository.findByTypeAndCoop(WalletType.USER, coop, Pageable.unpaged()).size
        val approvedDeposits = depositRepository.countUsersWithApprovedDeposit(coop)
        val investedWallets = blockchainService.getUserWalletsWithInvestment(coop).size
        return StatsResponse(initializedWallets, approvedDeposits, investedWallets)
    }
}
