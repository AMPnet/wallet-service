package com.ampnet.walletservice.service.pojo

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.walletservice.persistence.model.Wallet

data class OrganizationWithWallet(val organization: OrganizationResponse, val wallet: Wallet)
