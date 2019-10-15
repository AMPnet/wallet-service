package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.Deposit
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DepositRepository : JpaRepository<Deposit, Int> {
    @Query("SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file WHERE deposit.approved = ?1")
    fun findAllWithFile(approved: Boolean): List<Deposit>

    fun findByReference(reference: String): Optional<Deposit>
    fun findByUserUuid(userUuid: UUID): List<Deposit>
}
