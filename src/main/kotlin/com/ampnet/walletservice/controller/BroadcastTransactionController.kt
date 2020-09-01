package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.TxBroadcastRequest
import com.ampnet.walletservice.controller.pojo.response.TxHashResponse
import com.ampnet.walletservice.service.BroadcastTransactionService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BroadcastTransactionController(
    private val broadcastService: BroadcastTransactionService
) {

    companion object : KLogging()

    @PostMapping("/tx_broadcast")
    fun broadcastTransaction(@RequestBody request: TxBroadcastRequest): ResponseEntity<TxHashResponse> {
        logger.info { "Received request to broadcast transaction with txId: ${request.txId}" }
        val txHash = broadcastService.broadcast(request.txId, request.txSig)
        return ResponseEntity.ok(TxHashResponse(txHash))
    }
}
