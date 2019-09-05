package com.ampnet.walletservice.service.pojo

import com.ampnet.project.proto.OrganizationResponse
import com.ampnet.walletservice.persistence.model.Wallet

// TODO: add more organization data
data class OrganizationWithWallet(val uuid: String, val name: String, val wallet: Wallet) {
    constructor(organization: OrganizationResponse, wallet: Wallet) : this(
        organization.uuid, organization.name, wallet
    )
}
