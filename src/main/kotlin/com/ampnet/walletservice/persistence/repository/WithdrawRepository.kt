package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.persistence.model.Withdraw
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface WithdrawRepository : JpaRepository<Withdraw, Int> {

    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.ownerUuid = :owner AND withdraw.file IS NULL ORDER BY withdraw.createdAt DESC"
    )
    fun findPendingForOwner(owner: UUID): List<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NULL " +
            "AND withdraw.coop = :coop AND (:type IS NULL OR withdraw.type = :type)"
    )
    fun findAllApproved(coop: String, type: DepositWithdrawType?, pageable: Pageable): Page<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NOT NULL " +
            "AND withdraw.coop = :coop AND (:type IS NULL OR withdraw.type = :type)"
    )
    fun findAllBurned(coop: String, type: DepositWithdrawType?, pageable: Pageable): Page<Withdraw>

    fun findByOwnerUuid(owner: UUID): List<Withdraw>

    fun findByIdAndCoop(id: Int, coop: String): Optional<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw LEFT JOIN FETCH withdraw.file " +
            "WHERE withdraw.coop = :coop"
    )
    fun findAllWithFile(coop: String): List<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL " +
            "AND (withdraw.burnedTxHash IS NULL OR (withdraw.burnedTxHash IS NOT NULL AND withdraw.file IS NULL))" +
            "AND withdraw.coop = :coop AND (:type IS NULL OR withdraw.type = :type)"
    )
    fun findAllPending(coop: String, type: DepositWithdrawType?, pageable: Pageable): Page<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw LEFT JOIN FETCH withdraw.file " +
            "WHERE withdraw.approvedTxHash = :txHash AND withdraw.ownerUuid = :ownerUuid"
    )
    fun findByApprovedTxHashAndOwnerUuid(txHash: String, ownerUuid: UUID): Optional<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw LEFT JOIN FETCH withdraw.file " +
            "WHERE withdraw.ownerUuid = :ownerUuid"
    )
    fun findAllByOwnerUuid(ownerUuid: UUID): List<Withdraw>
}
