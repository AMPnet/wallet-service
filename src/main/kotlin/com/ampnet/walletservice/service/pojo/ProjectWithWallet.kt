package com.ampnet.walletservice.service.pojo

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.walletservice.persistence.model.Wallet

data class ProjectWithWallet(val project: ProjectResponse, val wallet: Wallet, val balance: Long? = null)
