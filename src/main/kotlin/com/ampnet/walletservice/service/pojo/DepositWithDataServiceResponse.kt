package com.ampnet.walletservice.service.pojo

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.persistence.model.Deposit
import java.time.ZonedDateTime
import java.util.UUID

data class DepositServiceResponse(
    val id: Int,
    val owner: UUID,
    val reference: String,
    val createdAt: ZonedDateTime,
    val createdBy: UUID,
    val type: DepositWithdrawType,
    val approvedAt: ZonedDateTime?,
    val amount: Long?,
    val txHash: String?,
    val declinedAt: ZonedDateTime?,
    val declinedComment: String?,
    val documentResponse: DocumentServiceResponse?
) {
    constructor(deposit: Deposit, withDocument: Boolean = false) : this(
        deposit.id,
        deposit.ownerUuid,
        deposit.reference,
        deposit.createdAt,
        deposit.createdBy,
        deposit.type,
        deposit.approvedAt,
        deposit.amount,
        deposit.txHash,
        deposit.declined?.createdAt,
        deposit.declined?.comment,
        if (withDocument) deposit.file?.let { DocumentServiceResponse(it) }
        else null
    )
}

data class DepositWithDataServiceResponse(
    val deposit: DepositServiceResponse,
    val user: UserServiceResponse?,
    val project: ProjectServiceResponse?
) {
    constructor(
        deposit: Deposit,
        userResponse: UserResponse?,
        projectResponse: ProjectResponse?,
        withDocument: Boolean = false
    ) : this (
        DepositServiceResponse(deposit, withDocument),
        userResponse?.let { UserServiceResponse(it) },
        projectResponse?.let { ProjectServiceResponse(it) }
    )
}

data class DepositListServiceResponse(
    val deposits: List<DepositWithDataServiceResponse>,
    val page: Int,
    val totalPages: Int
)
