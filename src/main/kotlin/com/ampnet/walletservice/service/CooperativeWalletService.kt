package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.controller.pojo.request.WalletTransferRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.service.pojo.OrganizationWithWallet
import com.ampnet.walletservice.service.pojo.ProjectWithWallet
import com.ampnet.walletservice.service.pojo.TransferOwnershipRequest
import com.ampnet.walletservice.service.pojo.UserWithWallet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CooperativeWalletService {
    fun generateWalletActivationTransaction(walletUuid: UUID, user: UserPrincipal): TransactionDataAndInfo
    fun activateWallet(walletUuid: UUID, signedTransaction: String): Wallet
    fun getAllUserWithUnactivatedWallet(pageable: Pageable, coop: String): Page<UserWithWallet>
    fun getOrganizationsWithUnactivatedWallet(pageable: Pageable, coop: String): Page<OrganizationWithWallet>
    fun getProjectsWithUnactivatedWallet(pageable: Pageable, coop: String): Page<ProjectWithWallet>
    fun generateSetTransferOwnership(owner: UserPrincipal, request: WalletTransferRequest): TransactionDataAndInfo
    fun transferOwnership(request: TransferOwnershipRequest): String
}
