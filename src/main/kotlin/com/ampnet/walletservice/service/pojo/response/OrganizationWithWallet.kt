package com.ampnet.walletservice.service.pojo.response

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.walletservice.persistence.model.Wallet

data class OrganizationWithWallet(val organization: OrganizationResponse, val wallet: Wallet)
