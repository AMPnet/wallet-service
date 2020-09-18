package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.persistence.model.Deposit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface DepositRepository : JpaRepository<Deposit, Int> {
    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file LEFT JOIN FETCH deposit.declined " +
            "WHERE deposit.approved = :approved AND deposit.type = :type AND deposit.coop = :coop",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.approved = :approved AND deposit.type = :type AND deposit.coop = :coop"
    )
    fun findAllWithFile(
        @Param("approved") approved: Boolean,
        type: DepositWithdrawType,
        coop: String,
        pageable: Pageable
    ): Page<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file LEFT JOIN FETCH deposit.declined " +
            "WHERE deposit.approved = true AND deposit.type = :type " +
            "AND deposit.txHash IS NOT NULL AND deposit.coop = :coop",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.approved = true AND deposit.type = :type " +
            "AND deposit.txHash IS NOT NULL AND deposit.coop = :coop"
    )
    fun findApprovedUnsignedWithFile(type: DepositWithdrawType, coop: String, pageable: Pageable): Page<Deposit>

    fun findByReference(reference: String): Optional<Deposit>
    fun findByOwnerUuid(ownerUuid: UUID): List<Deposit>

    @Query(
        "SELECT COUNT(DISTINCT deposit.ownerUuid) FROM Deposit deposit " +
            "WHERE deposit.approved = true AND deposit.type = 'USER'"
    )
    fun countUsersWithApprovedDeposit(): Int
}
