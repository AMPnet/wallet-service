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
            "WHERE deposit.approved = :approved AND deposit.type = :type",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.approved = :approved AND deposit.type = :type"
    )
    fun findAllWithFile(
        @Param("approved") approved: Boolean,
        type: DepositWithdrawType,
        pageable: Pageable
    ): Page<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file LEFT JOIN FETCH deposit.declined " +
            "WHERE deposit.approved = true AND deposit.type = :type AND deposit.txHash IS NOT NULL",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.approved = true AND deposit.type = :type AND deposit.txHash IS NOT NULL"
    )
    fun findApprovedUnsignedWithFile(type: DepositWithdrawType, pageable: Pageable): Page<Deposit>

    fun findByReference(reference: String): Optional<Deposit>
    fun findByOwnerUuid(ownerUuid: UUID): List<Deposit>

    @Query(
        "SELECT COUNT(DISTINCT deposit.ownerUuid) FROM Deposit deposit " +
            "WHERE deposit.approved = true AND deposit.type = 'USER'"
    )
    fun countUsersWithApprovedDeposit(): Int
}
