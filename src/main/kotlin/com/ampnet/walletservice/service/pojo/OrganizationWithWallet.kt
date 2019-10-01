package com.ampnet.walletservice.service.pojo

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.walletservice.controller.pojo.response.OrganizationControllerResponse
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.persistence.model.Wallet

data class OrganizationWithWallet(val organization: OrganizationControllerResponse, val wallet: WalletResponse) {
    constructor(organization: OrganizationResponse, wallet: Wallet) : this(
        OrganizationControllerResponse(organization), WalletResponse(wallet)
    )
}
