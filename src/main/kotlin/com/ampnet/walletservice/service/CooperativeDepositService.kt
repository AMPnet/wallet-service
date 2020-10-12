package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.pojo.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CooperativeDepositService {
    fun approve(request: ApproveDepositRequest): Deposit
    fun decline(id: Int, user: UUID, comment: String): Deposit
    fun getApprovedWithDocuments(type: DepositWithdrawType, pageable: Pageable): Page<Deposit>
    fun getUnapproved(type: DepositWithdrawType, pageable: Pageable): Page<Deposit>
    fun findByReference(reference: String): Deposit?
    fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo
    fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit
    fun countUsersWithApprovedDeposit(): Int
}
