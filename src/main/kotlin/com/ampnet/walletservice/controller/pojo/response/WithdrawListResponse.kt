package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse

data class WithdrawListResponse(val withdraws: List<WithdrawServiceResponse>)
