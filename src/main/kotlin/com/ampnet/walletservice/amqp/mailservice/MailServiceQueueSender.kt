package com.ampnet.walletservice.amqp.mailservice

import mu.KLogging
import org.springframework.amqp.AmqpException
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Profile("!test")
class MailServiceQueueSender(private val rabbitTemplate: RabbitTemplate) : MailService {

    companion object : KLogging()

    @Bean
    fun mailDeposit(): Queue = Queue(QUEUE_MAIL_DEPOSIT)

    @Bean
    fun mailWithdraw(): Queue = Queue(QUEUE_MAIL_WITHDRAW)

    @Bean
    fun mailWithdrawInfo(): Queue = Queue(QUEUE_MAIL_WITHDRAW_INFO)

    @Bean
    fun mailWalletActivated(): Queue = Queue(QUEUE_MAIL_WALLET_ACTIVATED)

    override fun sendDepositInfo(user: UUID, minted: Boolean) {
        val message = DepositInfoRequest(user, minted)
        sendMessage(QUEUE_MAIL_DEPOSIT, message)
    }

    override fun sendWithdrawRequest(user: UUID, amount: Long) {
        val message = WithdrawRequest(user, amount)
        sendMessage(QUEUE_MAIL_WITHDRAW, message)
    }

    override fun sendWithdrawInfo(user: UUID, burned: Boolean) {
        val message = WithdrawInfoRequest(user, burned)
        sendMessage(QUEUE_MAIL_WITHDRAW_INFO, message)
    }

    override fun sendWalletActivated(walletType: WalletTypeAmqp, walletOwner: UUID, activationData: String) {
        val message = WalletActivatedRequest(walletType, walletOwner, activationData)
        sendMessage(QUEUE_MAIL_WALLET_ACTIVATED, message)
    }

    override fun sendNewWalletMail(walletType: WalletTypeAmqp, coop: String, activationData: String) {
        val message = NewWalletRequest(walletType, coop, activationData)
        sendMessage(QUEUE_MAIL_WALLET_NEW, message)
    }

    private fun sendMessage(queue: String, message: Any) {
        try {
            logger.debug { "Sending to queue: $queue, message: $message" }
            rabbitTemplate.convertAndSend(queue, message)
        } catch (ex: AmqpException) {
            logger.warn(ex) { "Failed to send AMQP message to queue: $queue" }
        }
    }
}

const val QUEUE_MAIL_DEPOSIT = "mail.wallet.deposit"
const val QUEUE_MAIL_WITHDRAW = "mail.wallet.withdraw"
const val QUEUE_MAIL_WITHDRAW_INFO = "mail.wallet.withdraw-info"
const val QUEUE_MAIL_WALLET_ACTIVATED = "mail.wallet.activated"
const val QUEUE_MAIL_WALLET_NEW = "mail.wallet.new"
