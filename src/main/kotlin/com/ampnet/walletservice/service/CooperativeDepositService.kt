package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.pojo.request.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.request.MintServiceRequest
import com.ampnet.walletservice.service.pojo.response.DepositListServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositWithDataServiceResponse
import org.springframework.data.domain.Pageable

interface CooperativeDepositService {
    fun approve(request: ApproveDepositRequest): DepositServiceResponse
    fun delete(id: Int, user: UserPrincipal)
    fun getApprovedWithDocuments(
        coop: String,
        type: DepositWithdrawType?,
        pageable: Pageable
    ): DepositListServiceResponse

    fun getUnapproved(coop: String, type: DepositWithdrawType?, pageable: Pageable): DepositListServiceResponse
    fun findByReference(coop: String, reference: String): DepositWithDataServiceResponse?
    fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo
    fun confirmMintTransaction(coop: String, signedTransaction: String, depositId: Int): Deposit
    fun countUsersWithApprovedDeposit(coop: String): Int
    fun getByIdForCoop(coop: String, id: Int): DepositWithDataServiceResponse?
}
