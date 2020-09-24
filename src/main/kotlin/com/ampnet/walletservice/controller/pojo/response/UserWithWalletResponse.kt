package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.UserWithWallet
import org.springframework.data.domain.Page

data class UserWithWalletResponse(
    val user: UserControllerResponse,
    val wallet: WalletResponse
) {
    constructor(userWithWallet: UserWithWallet) : this(
        UserControllerResponse(userWithWallet.userResponse),
        WalletResponse(userWithWallet.wallet)
    )
}

data class UserWithWalletListResponse(
    val users: List<UserWithWalletResponse>,
    val page: Int,
    val totalPages: Int
) {
    constructor(usersPage: Page<UserWithWallet>) : this(
        usersPage.toList().map { UserWithWalletResponse(it) },
        usersPage.number,
        usersPage.totalPages
    )
}
