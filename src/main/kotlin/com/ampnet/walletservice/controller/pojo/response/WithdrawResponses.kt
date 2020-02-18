package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.persistence.model.Withdraw
import java.time.ZonedDateTime
import java.util.UUID

data class WithdrawResponse(
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
    val documentResponse: DocumentResponse?
) {
    constructor(withdraw: Withdraw) : this (
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
        withdraw.file?.let { DocumentResponse(it) }
    )
}

data class WithdrawWithUserResponse(
    val id: Int,
    val user: UserControllerResponse?,
    val amount: Long,
    val approvedTxHash: String?,
    val approvedAt: ZonedDateTime?,
    val burnedTxHash: String?,
    val burnedBy: UUID?,
    val burnedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
    val bankAccount: String,
    val userWallet: String,
    val documentResponse: DocumentResponse?
) {
    constructor(withdraw: Withdraw, user: UserResponse?, userWallet: String) : this(
        withdraw.id,
        user?.let { UserControllerResponse(it) },
        withdraw.amount,
        withdraw.approvedTxHash,
        withdraw.approvedAt,
        withdraw.burnedTxHash,
        withdraw.burnedBy,
        withdraw.burnedAt,
        withdraw.createdAt,
        withdraw.bankAccount,
        userWallet,
        withdraw.file?.let { DocumentResponse(it) }
    )
}
data class WithdrawWithUserListResponse(
    val withdraws: List<WithdrawWithUserResponse>,
    val page: Int,
    val totalPages: Int
)

// TODO: add WithdrawWithProjectResponse
