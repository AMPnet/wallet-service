package com.ampnet.walletservice.service.pojo

import java.util.UUID

data class MintServiceRequest(val depositId: Int, val byUser: UUID)
