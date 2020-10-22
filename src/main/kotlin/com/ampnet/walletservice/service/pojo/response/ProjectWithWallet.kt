package com.ampnet.walletservice.service.pojo.response

import com.ampnet.walletservice.persistence.model.Wallet

data class ProjectWithWallet(
    val project: ProjectServiceResponse,
    val wallet: Wallet,
    val balance: Long? = null,
    val payoutInProcess: Boolean? = null
)
