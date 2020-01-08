package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.Deposit
import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DepositRepository : JpaRepository<Deposit, Int> {
    @Query("SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file WHERE deposit.approved = :approved",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit WHERE deposit.approved = :approved")
    fun findAllWithFile(@Param("approved") approved: Boolean, pageable: Pageable): Page<Deposit>

    fun findByReference(reference: String): Optional<Deposit>
    fun findByUserUuid(userUuid: UUID): List<Deposit>

    @Query("SELECT COUNT(DISTINCT deposit.userUuid) FROM Deposit deposit WHERE deposit.approved = true")
    fun countUsersWithApprovedDeposit(): Int
}
