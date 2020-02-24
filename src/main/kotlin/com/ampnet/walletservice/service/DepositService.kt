package com.ampnet.walletservice.service

import com.ampnet.walletservice.persistence.model.Deposit
import java.util.UUID

interface DepositService {
    fun create(user: UUID, amount: Long): Deposit
    fun getPendingForUser(user: UUID): Deposit?
}
