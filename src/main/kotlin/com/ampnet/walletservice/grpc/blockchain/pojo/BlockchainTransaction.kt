package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.crowdfunding.proto.TransactionsResponse
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class BlockchainTransaction(
    val fromTxHash: String,
    val toTxHash: String,
    val amount: Long,
    val type: TransactionsResponse.Transaction.Type,
    val date: ZonedDateTime
) {
    constructor(transaction: TransactionsResponse.Transaction) : this(
        transaction.fromTxHash,
        transaction.toTxHash,
        transaction.amount.toLong(),
        transaction.type,
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(transaction.date.toLong()), ZoneId.systemDefault())
    )
}
