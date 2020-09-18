package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.pojo.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.GetDepositsServiceRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CooperativeDepositService {
    fun approve(request: ApproveDepositRequest): Deposit
    fun decline(id: Int, user: UUID, comment: String): Deposit
    fun getAllWithDocuments(request: GetDepositsServiceRequest, pageable: Pageable): Page<Deposit>
    fun getUnsigned(type: DepositWithdrawType, coop: String, pageable: Pageable): Page<Deposit>
    fun findByReference(reference: String, coop: String): Deposit?
    fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo
    fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit
    fun countUsersWithApprovedDeposit(coop: String): Int
}
