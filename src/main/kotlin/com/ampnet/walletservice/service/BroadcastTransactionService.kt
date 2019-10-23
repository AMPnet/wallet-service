package com.ampnet.walletservice.service

interface BroadcastTransactionService {
    fun broadcast(txId: Int, signedTransaction: String): String
}
