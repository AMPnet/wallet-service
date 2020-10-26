package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.enums.TransactionType
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.TransactionInfo

data class TransactionResponse(
    val tx: String,
    val txId: Int,
    val coop: String,
    val info: TransactionInfoResponse
) {
    constructor(transaction: TransactionDataAndInfo) : this(
        transaction.transactionData.tx,
        transaction.transactionInfo.id,
        transaction.transactionInfo.coop,
        TransactionInfoResponse(transaction.transactionInfo)
    )
}

data class TransactionInfoResponse(
    val txType: TransactionType,
    val title: String,
    val description: String
) {
    constructor(transactionInfo: TransactionInfo) : this(
        transactionInfo.type,
        transactionInfo.type.title,
        transactionInfo.description
    )
}
