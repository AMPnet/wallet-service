package com.ampnet.walletservice.service.pojo

import com.ampnet.walletservice.enums.TransactionType
import java.util.UUID

data class CreateTransactionRequest(
    val type: TransactionType,
    val title: String,
    val description: String,
    val userUuid: UUID,
    val companionData: String? = null
)
