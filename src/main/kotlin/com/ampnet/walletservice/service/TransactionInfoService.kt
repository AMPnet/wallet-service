package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.controller.pojo.request.WalletTransferRequest
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import com.ampnet.walletservice.service.pojo.RevenuePayoutTxInfo
import java.util.UUID

interface TransactionInfoService {
    fun activateWalletTransaction(walletUuid: UUID, walletType: WalletType, user: UserPrincipal): TransactionInfo
    fun createOrgTransaction(organization: UUID, organizationName: String, user: UserPrincipal): TransactionInfo
    fun createProjectTransaction(project: UUID, projectName: String, user: UserPrincipal): TransactionInfo
    fun createInvestTransaction(projectName: String, amount: Long, user: UserPrincipal): TransactionInfo
    fun cancelInvestmentTransaction(projectName: String, user: UserPrincipal): TransactionInfo
    fun createMintTransaction(request: MintServiceRequest, receivingWallet: String): TransactionInfo
    fun createApprovalTransaction(amount: Long, user: UserPrincipal, withdrawId: Int): TransactionInfo
    fun createBurnTransaction(amount: Long, user: UserPrincipal, withdrawId: Int): TransactionInfo
    fun createRevenuePayoutTransaction(request: RevenuePayoutTxInfo): TransactionInfo
    fun createTransferOwnership(owner: UserPrincipal, request: WalletTransferRequest): TransactionInfo
    fun deleteTransaction(id: Int)
    fun findTransactionInfo(id: Int): TransactionInfo?
}
