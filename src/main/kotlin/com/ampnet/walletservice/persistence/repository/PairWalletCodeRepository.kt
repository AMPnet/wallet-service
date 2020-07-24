package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.PairWalletCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PairWalletCodeRepository : JpaRepository<PairWalletCode, Int> {
    fun findByPublicKey(publicKey: String): Optional<PairWalletCode>
    fun findByCode(code: String): Optional<PairWalletCode>
}
