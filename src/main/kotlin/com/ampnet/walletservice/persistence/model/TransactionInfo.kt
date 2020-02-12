package com.ampnet.walletservice.persistence.model

import com.ampnet.walletservice.enums.TransactionType
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "transaction_info")
data class TransactionInfo(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val type: TransactionType,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val userUuid: UUID,

    @Column(nullable = true, length = 128)
    val companionData: String?
)
