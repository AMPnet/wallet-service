package com.ampnet.walletservice.service.pojo.response

import com.ampnet.walletservice.persistence.model.Withdraw
import java.time.ZonedDateTime
import java.util.UUID

data class WithdrawServiceResponse(
    val id: Int,
    val owner: UUID,
    val amount: Long,
    val approvedTxHash: String?,
    val approvedAt: ZonedDateTime?,
    val burnedTxHash: String?,
    val burnedBy: UUID?,
    val burnedAt: ZonedDateTime?,
    val bankAccount: String,
    val createdAt: ZonedDateTime,
    val documentResponse: DocumentServiceResponse?
) {
    constructor(withdraw: Withdraw, withDocument: Boolean = false) : this(
        withdraw.id,
        withdraw.ownerUuid,
        withdraw.amount,
        withdraw.approvedTxHash,
        withdraw.approvedAt,
        withdraw.burnedTxHash,
        withdraw.burnedBy,
        withdraw.burnedAt,
        withdraw.bankAccount,
        withdraw.createdAt,
        if (withDocument) withdraw.file?.let { DocumentServiceResponse(it) } else null
    )
}

data class WithdrawWithDataServiceResponse(
    val withdraw: WithdrawServiceResponse,
    val user: UserServiceResponse?,
    val project: ProjectServiceResponse?,
    val walletHash: String
) {
    constructor(
        withdraw: Withdraw,
        user: UserServiceResponse?,
        project: ProjectServiceResponse?,
        walletHash: String,
        withDocument: Boolean = false
    ) : this(
        WithdrawServiceResponse(withdraw, withDocument),
        user,
        project,
        walletHash
    )
}

data class WithdrawListServiceResponse(
    val withdraws: List<WithdrawWithDataServiceResponse>,
    val page: Int,
    val totalPages: Int
)
