package com.ampnet.walletservice.amqp.blockchainservice

import com.ampnet.walletservice.service.CooperativeWalletService
import mu.KLogging
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class BlockchainServiceQueueListeners(private val cooperativeWalletService: CooperativeWalletService) {

    companion object : KLogging()

    @Bean
    fun activateWalletQueue(): Queue = Queue(QUEUE_MIDDLEWARE_ACTIVATE_WALLET)

    @Bean
    fun updateCoopRolesQueue(): Queue = Queue(QUEUE_MIDDLEWARE_UPDATE_COOP_ROLES)

    @RabbitListener(queues = [QUEUE_MIDDLEWARE_ACTIVATE_WALLET])
    fun handleActivateWallet(message: ActivateWalletMessage) {
        logger.debug { "Received message: $message" }
        cooperativeWalletService.activateAdminWallet(message.address, message.coop, message.hash)
    }

    @RabbitListener(queues = [QUEUE_MIDDLEWARE_UPDATE_COOP_ROLES])
    fun handleUpdateCoopRoles(message: UpdateCoopRolesMessage) {
        logger.debug { "Received message: $message" }
        cooperativeWalletService.updateCoopUserRoles(message.coop)
    }
}

const val QUEUE_MIDDLEWARE_ACTIVATE_WALLET = "wallet.middleware.activate-wallet"
const val QUEUE_MIDDLEWARE_UPDATE_COOP_ROLES = "wallet.middleware.update-coop-roles"
