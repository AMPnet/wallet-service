package com.ampnet.walletservice.service.pojo.request

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.TransactionType

data class CreateTransactionRequest(
    val type: TransactionType,
    val description: String,
    val user: UserPrincipal,
    val companionData: String? = null
)
