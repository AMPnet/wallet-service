package com.ampnet.walletservice.service.pojo

import com.ampnet.walletservice.enums.DepositWithdrawType
import org.springframework.data.domain.Pageable

data class GetDepositsServiceRequest(
    val approved: Boolean,
    val type: DepositWithdrawType,
    val coop: String,
    val pageable: Pageable
)
