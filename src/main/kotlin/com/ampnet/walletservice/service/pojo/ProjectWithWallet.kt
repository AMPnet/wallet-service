package com.ampnet.walletservice.service.pojo

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectControllerResponse
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.persistence.model.Wallet

data class ProjectWithWallet(val project: ProjectControllerResponse, val wallet: WalletResponse) {
    constructor(project: ProjectResponse, wallet: Wallet) : this(
        ProjectControllerResponse(project), WalletResponse(wallet)
    )
    constructor(project: ProjectResponse, wallet: Wallet, balance: Long?) : this(
        ProjectControllerResponse(project), WalletResponse(wallet, balance)
    )
}
