package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.Declined
import org.springframework.data.jpa.repository.JpaRepository

interface DeclinedRepository : JpaRepository<Declined, Int>
