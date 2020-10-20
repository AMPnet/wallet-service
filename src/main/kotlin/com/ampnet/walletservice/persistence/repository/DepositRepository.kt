package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.persistence.model.Deposit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface DepositRepository : JpaRepository<Deposit, Int> {
    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.type = :type AND deposit.txHash IS NOT NULL",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.type = :type AND deposit.txHash IS NOT NULL"
    )
    fun findAllApprovedWithFileByType(type: DepositWithdrawType, pageable: Pageable): Page<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.txHash IS NOT NULL",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.txHash IS NOT NULL"
    )
    fun findAllApprovedWithFile(pageable: Pageable): Page<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit " +
            "WHERE deposit.type = :type AND deposit.txHash IS NULL AND deposit.declined is NULL",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.type = :type AND deposit.txHash IS NULL AND deposit.declined is NULL"
    )
    fun findAllUnapprovedByType(type: DepositWithdrawType, pageable: Pageable): Page<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit " +
            "WHERE deposit.txHash IS NULL AND deposit.declined is NULL",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.txHash IS NULL AND deposit.declined is NULL"
    )
    fun findAllUnapproved(pageable: Pageable): Page<Deposit>

    fun findByReference(reference: String): Optional<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.ownerUuid = :ownerUuid AND deposit.txHash IS NULL AND deposit.declined is NULL"
    )
    fun findByOwnerUuidUnsigned(ownerUuid: UUID): List<Deposit>

    @Query(
        "SELECT COUNT(DISTINCT deposit.ownerUuid) FROM Deposit deposit " +
            "WHERE deposit.txHash IS NOT NULL AND deposit.type = 'USER'"
    )
    fun countUsersWithApprovedDeposit(): Int

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.id = :depositId"
    )
    fun findWithFileById(depositId: Int): Optional<Deposit>

    @Query("SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file")
    fun findAllWithFile(): List<Deposit>
}
