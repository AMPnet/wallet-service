package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.RevenuePayout
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface RevenuePayoutRepository : JpaRepository<RevenuePayout, Int> {
    fun findByProjectUuid(projectUuid: UUID, pageable: Pageable): Page<RevenuePayout>
}
