package com.ampnet.walletservice.amqp.mailservice

import java.util.UUID

interface MailService {
    fun sendDepositInfo(user: UUID, minted: Boolean)
    fun sendWithdrawRequest(user: UUID, amount: Long)
    fun sendWithdrawInfo(user: UUID, burned: Boolean)
    fun sendWalletActivated(walletType: WalletTypeAmqp, walletOwner: UUID, activationData: String)
    fun sendNewWalletMail(walletType: WalletTypeAmqp, coop: String, activationData: String)
}
