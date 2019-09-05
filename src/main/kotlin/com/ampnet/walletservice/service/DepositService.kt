package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.pojo.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import java.util.UUID

interface DepositService {
    fun create(user: UUID, amount: Long): Deposit
    fun delete(id: Int)
    fun approve(request: ApproveDepositRequest): Deposit
    fun getAllWithDocuments(approved: Boolean): List<Deposit>
    fun findByReference(reference: String): Deposit?
    fun getPendingForUser(user: UUID): Deposit?
    fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo
    fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit
}
