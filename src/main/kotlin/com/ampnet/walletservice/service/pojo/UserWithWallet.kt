package com.ampnet.walletservice.service.pojo

import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.persistence.model.Wallet

data class UserWithWallet(val userResponse: UserResponse, val wallet: Wallet)
