package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.controller.pojo.request.WalletTransferRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.service.pojo.request.TransferOwnershipRequest
import com.ampnet.walletservice.service.pojo.response.OrganizationWithWallet
import com.ampnet.walletservice.service.pojo.response.ProjectWithWallet
import com.ampnet.walletservice.service.pojo.response.UserWithWallet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CooperativeWalletService {
    fun generateWalletActivationTransaction(walletUuid: UUID, user: UserPrincipal): TransactionDataAndInfo
    fun activateWallet(walletUuid: UUID, signedTransaction: String): Wallet
    fun getAllUserWithUnactivatedWallet(coop: String, pageable: Pageable): Page<UserWithWallet>
    fun getOrganizationsWithUnactivatedWallet(coop: String, pageable: Pageable): Page<OrganizationWithWallet>
    fun getProjectsWithUnactivatedWallet(coop: String, pageable: Pageable): Page<ProjectWithWallet>
    fun generateSetTransferOwnership(owner: UserPrincipal, request: WalletTransferRequest): TransactionDataAndInfo
    fun transferOwnership(request: TransferOwnershipRequest): String
}
