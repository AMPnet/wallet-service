package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.BankAccount
import org.springframework.data.jpa.repository.JpaRepository

interface BankAccountRepository : JpaRepository<BankAccount, Int> {
    fun findAllByCoop(coop: String): List<BankAccount>
}
