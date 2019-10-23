package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.ProjectWithWallet

data class ProjectWithWalletResponse(val project: ProjectControllerResponse, val wallet: WalletResponse) {
    constructor(projectWithWallet: ProjectWithWallet) : this(
        ProjectControllerResponse(projectWithWallet.project),
        WalletResponse(projectWithWallet.wallet, projectWithWallet.balance)
    )
}
data class ProjectWithWalletListResponse(val projects: List<ProjectWithWalletResponse>)
