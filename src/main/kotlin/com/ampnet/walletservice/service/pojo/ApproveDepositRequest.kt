package com.ampnet.walletservice.service.pojo

import com.ampnet.core.jwt.UserPrincipal

data class ApproveDepositRequest(
    val id: Int,
    val user: UserPrincipal,
    val amount: Long,
    val documentSaveRequest: DocumentSaveRequest
)
