package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.controller.pojo.request.WalletCreateRequest
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.PairWalletCode
import com.ampnet.walletservice.persistence.model.Wallet
import java.util.UUID

interface WalletService {
    fun getWallet(owner: UUID): Wallet?
    fun createUserWallet(user: UserPrincipal, request: WalletCreateRequest): Wallet
    fun generateTransactionToCreateProjectWallet(project: UUID, user: UserPrincipal): TransactionDataAndInfo
    fun createProjectWallet(project: UUID, signedTransaction: String, coop: String): Wallet
    fun generateTransactionToCreateOrganizationWallet(organization: UUID, user: UserPrincipal): TransactionDataAndInfo
    fun createOrganizationWallet(organization: UUID, signedTransaction: String, coop: String): Wallet
    fun generatePairWalletCode(publicKey: String): PairWalletCode
    fun getPairWalletCode(code: String): PairWalletCode?
}
