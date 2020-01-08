package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.service.pojo.OrganizationWithWallet
import com.ampnet.walletservice.service.pojo.ProjectWithWallet
import com.ampnet.walletservice.service.pojo.UserWithWallet
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CooperativeWalletService {
    fun generateWalletActivationTransaction(walletUuid: UUID, userUuid: UUID): TransactionDataAndInfo
    fun activateWallet(walletUuid: UUID, signedTransaction: String): Wallet
    fun getAllUserWithUnactivatedWallet(pageable: Pageable): Page<UserWithWallet>
    fun getOrganizationsWithUnactivatedWallet(pageable: Pageable): Page<OrganizationWithWallet>
    fun getProjectsWithUnactivatedWallet(pageable: Pageable): Page<ProjectWithWallet>
}
