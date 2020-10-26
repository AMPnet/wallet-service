package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.response.UserServiceResponse
import com.ampnet.walletservice.service.pojo.response.UserWithWallet
import org.springframework.data.domain.Page

data class UserWithWalletResponse(val user: UserServiceResponse, val wallet: WalletResponse) {
    constructor(userWithWallet: UserWithWallet) : this(
        userWithWallet.userResponse, WalletResponse(userWithWallet.wallet)
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
