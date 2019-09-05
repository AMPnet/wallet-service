package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.persistence.model.PairWalletCode

data class PairWalletResponse(
    val code: String,
    val publicKey: String
) {
    constructor(pairWalletCode: PairWalletCode) : this(
        pairWalletCode.code,
        pairWalletCode.publicKey
    )
}
