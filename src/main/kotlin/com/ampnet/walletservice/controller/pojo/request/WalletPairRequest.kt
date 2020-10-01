package com.ampnet.walletservice.controller.pojo.request

import javax.validation.constraints.Size

data class WalletPairRequest(@field:Size(max = 128) val publicKey: String)
