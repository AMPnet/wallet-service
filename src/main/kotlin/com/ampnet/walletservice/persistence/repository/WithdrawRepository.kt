package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.persistence.model.Withdraw
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface WithdrawRepository : JpaRepository<Withdraw, Int> {
    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NULL AND withdraw.type = :type"
    )
    fun findAllApprovedByType(type: DepositWithdrawType, pageable: Pageable): Page<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NULL"
    )
    fun findAllApproved(pageable: Pageable): Page<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NOT NULL AND withdraw.type = :type"
    )
    fun findAllBurnedByType(type: DepositWithdrawType, pageable: Pageable): Page<Withdraw>

    @Query(
        "SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NOT NULL"
    )
    fun findAllBurned(pageable: Pageable): Page<Withdraw>

    fun findByOwnerUuid(owner: UUID): List<Withdraw>

    @Query("SELECT withdraw FROM Withdraw withdraw LEFT JOIN FETCH withdraw.file")
    fun findAllWithFile(): List<Withdraw>
}
