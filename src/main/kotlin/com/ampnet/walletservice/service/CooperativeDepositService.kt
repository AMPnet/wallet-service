package com.ampnet.walletservice.service

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.pojo.request.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.request.MintServiceRequest
import com.ampnet.walletservice.service.pojo.response.DepositListServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositWithDataServiceResponse
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CooperativeDepositService {
    fun approve(request: ApproveDepositRequest): DepositServiceResponse
    fun decline(id: Int, user: UUID, comment: String): DepositServiceResponse
    fun getApprovedWithDocuments(type: DepositWithdrawType?, pageable: Pageable): DepositListServiceResponse
    fun getUnapproved(type: DepositWithdrawType?, pageable: Pageable): DepositListServiceResponse
    fun findByReference(reference: String): DepositWithDataServiceResponse?
    fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo
    fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit
    fun countUsersWithApprovedDeposit(): Int
    fun getById(id: Int): DepositWithDataServiceResponse?
}
