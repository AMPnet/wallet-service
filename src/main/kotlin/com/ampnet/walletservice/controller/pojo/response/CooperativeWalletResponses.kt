package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.OrganizationWithWallet
import com.ampnet.walletservice.service.pojo.ProjectWithWallet
import com.ampnet.walletservice.service.pojo.UserWithWallet

data class UserWithWalletResponse(val user: UserControllerResponse, val wallet: WalletResponse) {
    constructor(userWithWallet: UserWithWallet) : this(
        UserControllerResponse(userWithWallet.userResponse), WalletResponse(userWithWallet.wallet)
    )
}
data class UserWithWalletListResponse(val users: List<UserWithWalletResponse>)
data class OrganizationWithWalletListResponse(val organizations: List<OrganizationWithWallet>)
data class ProjectWithWalletListResponse(val projects: List<ProjectWithWallet>)
