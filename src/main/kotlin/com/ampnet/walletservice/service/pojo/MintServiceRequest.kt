package com.ampnet.walletservice.service.pojo

import com.ampnet.core.jwt.UserPrincipal

data class MintServiceRequest(val depositId: Int, val byUser: UserPrincipal)
