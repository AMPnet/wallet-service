package com.ampnet.walletservice.service.pojo.response

import com.ampnet.walletservice.persistence.model.Wallet

data class UserWithWallet(val userResponse: UserServiceResponse, val wallet: Wallet)
