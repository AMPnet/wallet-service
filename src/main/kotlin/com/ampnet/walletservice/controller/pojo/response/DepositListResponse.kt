package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse

data class DepositListResponse(val deposits: List<DepositServiceResponse>)
