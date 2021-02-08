package com.ampnet.walletservice.amqp.mailservice

import mu.KLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MailServiceQueueSender(private val rabbitTemplate: RabbitTemplate) : MailService {

    companion object : KLogging()

    override fun sendDepositInfo(user: UUID, minted: Boolean) {
        val message = DepositInfoRequest(user, minted)
        logger.debug { "Sending mail deposit info: $message" }
        rabbitTemplate.convertAndSend(QUEUE_MAIL_DEPOSIT, message)
    }

    override fun sendWithdrawRequest(user: UUID, amount: Long) {
        val message = WithdrawRequest(user, amount)
        logger.debug { "Sending mail withdraw request: $message" }
        rabbitTemplate.convertAndSend(QUEUE_MAIL_WITHDRAW, message)
    }

    override fun sendWithdrawInfo(user: UUID, burned: Boolean) {
        val message = WithdrawInfoRequest(user, burned)
        logger.debug { "Sending mail withdraw info: $message" }
        rabbitTemplate.convertAndSend(QUEUE_MAIL_WITHDRAW_INFO, message)
    }

    override fun sendWalletActivated(walletType: WalletTypeAmqp, walletOwner: UUID, activationData: String) {
        val message = WalletActivatedRequest(walletType, walletOwner, activationData)
        logger.debug { "Sending mail wallet activated: $message" }
        rabbitTemplate.convertAndSend(QUEUE_MAIL_WALLET_ACTIVATED, message)
    }

    override fun sendNewWalletMail(walletType: WalletTypeAmqp, coop: String, activationData: String) {
        val message = NewWalletRequest(walletType, coop, activationData)
        logger.debug { "Sending mail new wallet: $message" }
        rabbitTemplate.convertAndSend(QUEUE_MAIL_WALLET_NEW, message)
    }
}

const val QUEUE_MAIL_DEPOSIT = "mail.wallet.deposit"
const val QUEUE_MAIL_WITHDRAW = "mail.wallet.withdraw"
const val QUEUE_MAIL_WITHDRAW_INFO = "mail.wallet.withdraw-info"
const val QUEUE_MAIL_WALLET_ACTIVATED = "mail.wallet.activated"
const val QUEUE_MAIL_WALLET_NEW = "mail.wallet.new"
