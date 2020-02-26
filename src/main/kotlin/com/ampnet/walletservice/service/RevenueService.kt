package com.ampnet.walletservice.service

import com.ampnet.walletservice.grpc.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.walletservice.persistence.model.RevenuePayout
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface RevenueService {
    fun generateRevenuePayout(user: UUID, project: UUID, amount: Long): TransactionDataAndInfo
    fun confirmRevenuePayout(signedTransaction: String, revenuePayoutId: Int): RevenuePayout
    fun getRevenuePayouts(project: UUID, pageable: Pageable): Page<RevenuePayout>
}
