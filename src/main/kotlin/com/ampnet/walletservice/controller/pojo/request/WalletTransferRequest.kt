package com.ampnet.walletservice.controller.pojo.request

import com.ampnet.walletservice.enums.TransferWalletType

data class WalletTransferRequest(val walletAddress: String, val type: TransferWalletType)
