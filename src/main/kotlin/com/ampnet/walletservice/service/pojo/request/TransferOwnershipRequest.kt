package com.ampnet.walletservice.service.pojo.request

import com.ampnet.walletservice.enums.TransferWalletType
import java.util.UUID

data class TransferOwnershipRequest(
    val owner: UUID,
    val walletAddress: String,
    val type: TransferWalletType,
    val signedTransaction: String
)
