package com.ampnet.walletservice.service.pojo

import com.ampnet.project.proto.ProjectResponse
import com.ampnet.walletservice.persistence.model.Wallet

// TODO: add more project data
data class ProjectWithWallet(val uuid: String, val name: String, val wallet: Wallet) {
    constructor(project: ProjectResponse, wallet: Wallet) : this(project.uuid, project.name, wallet)
}
