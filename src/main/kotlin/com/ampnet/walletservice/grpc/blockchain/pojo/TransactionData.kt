package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.crowdfunding.proto.RawTxResponse
import com.ampnet.walletservice.persistence.model.TransactionInfo

data class TransactionData(val tx: String) {
    constructor(rawTxResponse: RawTxResponse) : this(rawTxResponse.tx)
}

data class TransactionDataAndInfo(val transactionData: TransactionData, val transactionInfo: TransactionInfo)
