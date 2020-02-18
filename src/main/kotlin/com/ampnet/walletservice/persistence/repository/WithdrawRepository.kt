package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Withdraw
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WithdrawRepository : JpaRepository<Withdraw, Int> {
    @Query("SELECT withdraw FROM Withdraw withdraw " +
        "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NULL AND withdraw.type = :type")
    fun findAllApproved(type: WalletType, pageable: Pageable): Page<Withdraw>

    @Query("SELECT withdraw FROM Withdraw withdraw " +
        "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NOT NULL AND withdraw.type = :type")
    fun findAllBurned(type: WalletType, pageable: Pageable): Page<Withdraw>

    fun findByOwnerUuid(owner: UUID): List<Withdraw>
}
