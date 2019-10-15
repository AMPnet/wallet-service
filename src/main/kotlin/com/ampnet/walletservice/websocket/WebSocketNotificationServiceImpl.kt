package com.ampnet.walletservice.websocket

import com.ampnet.walletservice.websocket.pojo.TxStatusResponse
import mu.KLogging
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class WebSocketNotificationServiceImpl(
    private val messagingTemplate: SimpMessagingTemplate
) : WebSocketNotificationService {

    companion object : KLogging()

    override fun notifyTxBroadcast(txId: Int, status: String) {
        val response = TxStatusResponse(txId, status)
        logger.debug { "Sending WebSocket notification: $response" }
        try {
            messagingTemplate.convertAndSend("/tx_status/$txId", response)
            logger.debug { "Successfully sent WebSocket notification: $response" }
        } catch (ex: MessagingException) {
            logger.warn(ex) { "Failed to send WebSocket notification: $response" }
        }
    }
}
