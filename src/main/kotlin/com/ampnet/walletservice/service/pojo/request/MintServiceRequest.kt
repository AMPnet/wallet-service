package com.ampnet.walletservice.service.pojo.request

import java.util.UUID

data class MintServiceRequest(val depositId: Int, val byUser: UUID)
