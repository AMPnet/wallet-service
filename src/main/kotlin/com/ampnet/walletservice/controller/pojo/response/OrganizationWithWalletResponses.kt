package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.OrganizationWithWallet

data class OrganizationWithWalletResponse(
    val organization: OrganizationControllerResponse,
    val wallet: WalletResponse
) {
    constructor(organizationWithWallet: OrganizationWithWallet): this(
        OrganizationControllerResponse(organizationWithWallet.organization),
        WalletResponse(organizationWithWallet.wallet)
    )
}
data class OrganizationWithWalletListResponse(val organizations: List<OrganizationWithWalletResponse>)
