package com.ampnet.walletservice.service

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
    fun generateWalletActivationTransaction(walletUuid: UUID, userUuid: UUID): TransactionDataAndInfo
    fun activateWallet(walletUuid: UUID, signedTransaction: String): Wallet
    fun getAllUserWithUnactivatedWallet(pageable: Pageable): Page<UserWithWallet>
    fun getOrganizationsWithUnactivatedWallet(pageable: Pageable): Page<OrganizationWithWallet>
    fun getProjectsWithUnactivatedWallet(pageable: Pageable): Page<ProjectWithWallet>
    fun generateSetTransferOwnership(owner: UUID, request: WalletTransferRequest): TransactionDataAndInfo
    fun transferOwnership(request: TransferOwnershipRequest): String
}
