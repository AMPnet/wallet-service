package com.ampnet.walletservice.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAllWallets() {
        em.createNativeQuery("DELETE FROM wallet").executeUpdate()
    }

    @Transactional
    fun deleteAllTransactionInfo() {
        em.createNativeQuery("DELETE FROM transaction_info ").executeUpdate()
    }

    @Transactional
    fun deleteAllPairWalletCodes() {
        em.createNativeQuery("DELETE FROM pair_wallet_code").executeUpdate()
    }

    @Transactional
    fun deleteAllWithdraws() {
        em.createNativeQuery("DELETE FROM withdraw").executeUpdate()
    }

    @Transactional
    fun deleteAllDeposits() {
        em.createNativeQuery("DELETE FROM deposit").executeUpdate()
    }
}
