package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.persistence.model.Deposit
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

@Suppress("TooManyFunctions")
interface DepositRepository : JpaRepository<Deposit, Int> {
    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.txHash IS NOT NULL AND deposit.coop = :coop AND (:type IS NULL OR deposit.type = :type)",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.txHash IS NOT NULL AND deposit.coop = :coop AND (:type IS NULL OR deposit.type = :type)"
    )
    fun findAllApprovedWithFile(coop: String, type: DepositWithdrawType?, pageable: Pageable): Page<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit " +
            "WHERE deposit.txHash IS NULL AND deposit.coop = :coop " +
            "AND (:type IS NULL OR deposit.type = :type)",
        countQuery = "SELECT COUNT(deposit) FROM Deposit deposit " +
            "WHERE deposit.txHash IS NULL AND deposit.coop = :coop " +
            "AND (:type IS NULL OR deposit.type = :type)"
    )
    fun findAllUnapproved(coop: String, type: DepositWithdrawType?, pageable: Pageable): Page<Deposit>

    fun findByCoopAndReference(coop: String, reference: String): Optional<Deposit>
    fun findByIdAndCoop(id: Int, coop: String): Optional<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.ownerUuid = :ownerUuid AND deposit.txHash IS NULL"
    )
    fun findByOwnerUuidUnsigned(ownerUuid: UUID): List<Deposit>

    @Query(
        "SELECT COUNT(DISTINCT deposit.ownerUuid) FROM Deposit deposit " +
            "WHERE deposit.txHash IS NOT NULL AND deposit.type = 'USER' AND deposit.coop = :coop"
    )
    fun countUsersWithApprovedDeposit(coop: String): Int

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.id = :depositId AND deposit.coop = :coop"
    )
    fun findWithFileById(coop: String, depositId: Int): Optional<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.coop = :coop"
    )
    fun findAllWithFile(coop: String): List<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.ownerUuid = :ownerUuid"
    )
    fun findAllByOwnerUuid(ownerUuid: UUID): List<Deposit>

    @Query(
        "SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.file " +
            "WHERE deposit.txHash = :txHash AND deposit.ownerUuid = :ownerUuid"
    )
    fun findByTxHashAndOwnerUuid(txHash: String, ownerUuid: UUID): Optional<Deposit>

    fun findByIdAndCreatedBy(id: Int, createdBy: UUID): Optional<Deposit>
}
