package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.TxBroadcastRequest
import com.ampnet.walletservice.controller.pojo.response.TxHashResponse
import com.ampnet.walletservice.service.BroadcastTransactionService
import com.ampnet.walletservice.websocket.WebSocketNotificationService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BroadcastTransactionController(
    private val broadcastService: BroadcastTransactionService,
    private val notificationService: WebSocketNotificationService
) {

    companion object : KLogging()

    @PostMapping("/tx_broadcast")
    fun broadcastTransaction(@RequestBody request: TxBroadcastRequest): ResponseEntity<TxHashResponse> {
        logger.info { "Received request to broadcast transaction with txId: ${request.txId}" }
        logger.debug { "Received request to broadcast transaction with txSig: ${request.txSig}" }
        val txHash = broadcastService.broadcast(request.txId, request.txSig)
        notificationService.notifyTxBroadcast(request.txId, "BROADCAST")
        return ResponseEntity.ok(TxHashResponse(txHash))
    }
}
