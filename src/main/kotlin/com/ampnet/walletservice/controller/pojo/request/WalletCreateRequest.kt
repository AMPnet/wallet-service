package com.ampnet.walletservice.controller.pojo.request

import javax.validation.constraints.Email
import javax.validation.constraints.Size

data class WalletCreateRequest(
    @field:Size(max = 128)
    val publicKey: String,
    @field:Email
    val email: String?,
    val providerId: String?
)
