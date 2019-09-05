package com.ampnet.walletservice.service.pojo

import java.util.UUID

data class ApproveDepositRequest(
    val id: Int,
    val user: UUID,
    val amount: Long,
    val documentSaveRequest: DocumentSaveRequest
)
