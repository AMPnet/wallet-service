package com.ampnet.walletservice.websocket

interface WebSocketNotificationService {
    fun notifyTxBroadcast(txId: Int, status: String)
}
