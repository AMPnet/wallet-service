package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.OrganizationWithWallet
import org.springframework.data.domain.Page

data class OrganizationWithWalletResponse(
    val organization: OrganizationControllerResponse,
    val wallet: WalletResponse,
    val coop: String
) {
    constructor(organizationWithWallet: OrganizationWithWallet) : this(
        OrganizationControllerResponse(organizationWithWallet.organization),
        WalletResponse(organizationWithWallet.wallet),
        organizationWithWallet.wallet.coop
    )
}

data class OrganizationWithWalletListResponse(
    val organizations: List<OrganizationWithWalletResponse>,
    val page: Int,
    val totalPages: Int
) {
    constructor(organizationWithWalletPage: Page<OrganizationWithWallet>) : this(
        organizationWithWalletPage.toList().map { OrganizationWithWalletResponse(it) },
        organizationWithWalletPage.number,
        organizationWithWalletPage.totalPages
    )
}
