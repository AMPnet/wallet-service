package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.TransactionInfo
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import java.util.UUID

interface TransactionInfoService {
    fun activateWalletTransaction(walletUuid: UUID, walletType: WalletType, userUuid: UUID): TransactionInfo
    fun createOrgTransaction(organization: UUID, organizationName: String, userUuid: UUID): TransactionInfo
    fun createProjectTransaction(project: UUID, projectName: String, userUuid: UUID): TransactionInfo
    fun createInvestTransaction(projectName: String, amount: Long, userUuid: UUID): TransactionInfo
    fun createMintTransaction(request: MintServiceRequest, receivingWallet: String): TransactionInfo
    fun createApprovalTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo
    fun createBurnTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo
    fun deleteTransaction(id: Int)
    fun findTransactionInfo(id: Int): TransactionInfo?
}
