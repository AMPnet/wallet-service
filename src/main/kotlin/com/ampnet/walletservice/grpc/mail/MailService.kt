package com.ampnet.walletservice.grpc.mail

import java.util.UUID

interface MailService {
    fun sendDepositRequest(user: UUID, amount: Long)
    fun sendDepositInfo(user: UUID, minted: Boolean)
    fun sendWithdrawRequest(user: UUID, amount: Long)
    fun sendWithdrawInfo(user: UUID, burned: Boolean)
    fun sendNewWalletMail()
}
