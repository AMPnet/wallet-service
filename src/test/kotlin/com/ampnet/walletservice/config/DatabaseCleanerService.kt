package com.ampnet.walletservice.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAllWallets() {
        deleteAllFromTable("wallet")
    }

    @Transactional
    fun deleteAllTransactionInfo() {
        deleteAllFromTable("transaction_info")
    }

    @Transactional
    fun deleteAllPairWalletCodes() {
        deleteAllFromTable("pair_wallet_code")
    }

    @Transactional
    fun deleteAllWithdraws() {
        deleteAllFromTable("withdraw")
    }

    @Transactional
    fun deleteAllDeposits() {
        deleteAllFromTable("deposit")
    }

    @Transactional
    fun deleteAllRevenuePayouts() {
        deleteAllFromTable("revenue_payout")
    }

    @Transactional
    fun deleteAllBankAccounts() {
        deleteAllFromTable("bank_account")
    }

    private fun deleteAllFromTable(name: String) {
        em.createNativeQuery("DELETE FROM $name").executeUpdate()
    }
}
