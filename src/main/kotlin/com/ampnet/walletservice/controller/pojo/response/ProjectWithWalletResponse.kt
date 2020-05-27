package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.ProjectWithWallet
import org.springframework.data.domain.Page

data class ProjectWithWalletResponse(
    val project: ProjectControllerResponse,
    val wallet: WalletResponse
) {
    constructor(projectWithWallet: ProjectWithWallet) : this(
        ProjectControllerResponse(projectWithWallet.project),
        WalletResponse(projectWithWallet.wallet)
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
