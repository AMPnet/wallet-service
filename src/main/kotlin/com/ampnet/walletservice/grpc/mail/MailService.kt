package com.ampnet.walletservice.grpc.mail

import com.ampnet.mailservice.proto.WalletTypeRequest
import java.util.UUID

interface MailService {
    fun sendDepositInfo(user: UUID, minted: Boolean)
    fun sendWithdrawRequest(user: UUID, amount: Long)
    fun sendWithdrawInfo(user: UUID, burned: Boolean)
    fun sendNewWalletMail(walletType: WalletTypeRequest.Type)
}
