package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import com.ampnet.walletservice.service.pojo.response.ProjectWithWallet
import org.springframework.data.domain.Page

data class ProjectWithWalletResponse(
    val project: ProjectServiceResponse,
    val wallet: WalletResponse,
    val payoutInProcess: Boolean?
) {
    constructor(projectWithWallet: ProjectWithWallet) : this(
        projectWithWallet.project,
        WalletResponse(projectWithWallet.wallet, projectWithWallet.balance),
        projectWithWallet.payoutInProcess
    )
}

data class ProjectWithWalletListResponse(
    val projects: List<ProjectWithWalletResponse>,
    val page: Int,
    val totalPages: Int
) {
    constructor(projectWithWalletPage: Page<ProjectWithWallet>) : this(
        projectWithWalletPage.toList().map { ProjectWithWalletResponse(it) },
        projectWithWalletPage.number,
        projectWithWalletPage.totalPages
    )
}
