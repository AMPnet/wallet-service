package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.PairWalletCode
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.service.pojo.ProjectWithWallet
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface WalletService {
    fun getWalletBalance(wallet: Wallet): Long?
    fun getWallet(owner: UUID): Wallet?
    fun createUserWallet(user: UUID, publicKey: String): Wallet
    fun generateTransactionToCreateProjectWallet(project: UUID, user: UUID): TransactionDataAndInfo
    fun createProjectWallet(project: UUID, signedTransaction: String): Wallet
    fun generateTransactionToCreateOrganizationWallet(organization: UUID, user: UUID): TransactionDataAndInfo
    fun createOrganizationWallet(organization: UUID, signedTransaction: String): Wallet
    fun generatePairWalletCode(publicKey: String): PairWalletCode
    fun getPairWalletCode(code: String): PairWalletCode?
    fun getProjectsWithActiveWallet(pageable: Pageable): Page<ProjectWithWallet>
}
