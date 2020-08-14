package com.ampnet.walletservice.controller.pojo.request

import com.ampnet.walletservice.enums.TransferWalletType
import java.util.UUID

data class WalletTransferRequest(val userUUID: UUID, val type: TransferWalletType)
