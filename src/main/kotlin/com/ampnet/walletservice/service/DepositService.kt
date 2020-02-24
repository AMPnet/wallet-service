package com.ampnet.walletservice.service

import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.pojo.DepositCreateServiceRequest
import java.util.UUID

interface DepositService {
    fun create(request: DepositCreateServiceRequest): Deposit
    fun getPendingForUser(user: UUID): Deposit?
}
