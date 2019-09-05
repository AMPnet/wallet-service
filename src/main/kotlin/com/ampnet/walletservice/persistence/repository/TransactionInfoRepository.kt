package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.TransactionInfo
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionInfoRepository : JpaRepository<TransactionInfo, Int>
