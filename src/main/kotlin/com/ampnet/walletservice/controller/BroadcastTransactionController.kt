package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TxHashResponse
import com.ampnet.walletservice.service.BroadcastTransactionService
import com.ampnet.walletservice.websocket.WebSocketNotificationService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class BroadcastTransactionController(
    private val broadcastService: BroadcastTransactionService,
    private val notificationService: WebSocketNotificationService
) {

    companion object : KLogging()

    @PostMapping("/tx_broadcast")
    fun broadcastTransaction(
        @RequestParam(name = "tx_id", required = true) txId: Int,
        @RequestParam(name = "tx_sig", required = true) signedTransaction: String
    ): ResponseEntity<TxHashResponse> {
        logger.info { "Received request to broadcast transaction with id: $txId" }
        logger.debug { "Received request to broadcast transaction with sig: $signedTransaction" }
        val txHash = broadcastService.broadcast(txId, signedTransaction)
        notificationService.notifyTxBroadcast(txId, "BROADCAST")
        return ResponseEntity.ok(TxHashResponse(txHash))
    }
}
