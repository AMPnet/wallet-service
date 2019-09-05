package com.ampnet.walletservice.persistence.repository

import com.ampnet.walletservice.persistence.model.File
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<File, Int>
