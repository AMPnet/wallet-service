package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.crowdfunding.proto.TransactionInfo
import com.ampnet.crowdfunding.proto.TransactionState
import com.ampnet.crowdfunding.proto.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class BlockchainTransaction(
    val fromTxHash: String,
    val toTxHash: String,
    val amount: Long,
    val type: TransactionType,
    val date: ZonedDateTime,
    val state: TransactionState,
    val txHash: String
) {
    var from: String? = null
    var to: String? = null
    var description: String? = null
    var share: String? = null

    constructor(transaction: TransactionInfo) : this(
        transaction.fromTxHash,
        transaction.toTxHash,
        transaction.amount.toLong(),
        transaction.type,
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(transaction.date.toLong()), ZoneId.systemDefault()),
        transaction.state,
        transaction.txHash
    )
}
