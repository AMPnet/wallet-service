package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface WalletRepository : JpaRepository<Wallet, UUID> {
    fun findByOwner(owner: UUID): Optional<Wallet>
    fun findByActivationData(activationData: String): Optional<Wallet>
    fun findByOwnerIn(owners: Collection<UUID>): List<Wallet>

    @Query(
        "SELECT wallet FROM Wallet wallet " +
            "WHERE wallet.type = ?1 AND wallet.hash IS NULL"
    )
    fun findUnactivatedByType(type: WalletType, pageable: Pageable): Page<Wallet>
    @Query(
        "SELECT wallet FROM Wallet wallet " +
            "WHERE wallet.type = ?1 AND wallet.hash IS NOT NULL"
    )
    fun findActivatedByType(type: WalletType, pageable: Pageable): Page<Wallet>
    @Query(
        "SELECT wallet FROM Wallet wallet " +
            "WHERE wallet.hash IN (:hashes)"
    )
    fun findByHashes(hashes: Iterable<String>): List<Wallet>
}
