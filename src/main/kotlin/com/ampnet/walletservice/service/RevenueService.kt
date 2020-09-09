package com.ampnet.walletservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.RevenuePayout
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface RevenueService {
    fun generateRevenuePayout(user: UserPrincipal, project: UUID, amount: Long): TransactionDataAndInfo
    fun confirmRevenuePayout(signedTransaction: String, revenuePayoutId: Int): RevenuePayout
    fun getRevenuePayouts(project: UUID, pageable: Pageable): Page<RevenuePayout>
}
