package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.persistence.model.Deposit
import java.time.ZonedDateTime
import java.util.UUID

data class DepositResponse(
    val id: Int,
    val owner: UUID,
    val reference: String,
    val approved: Boolean,
    val createdAt: ZonedDateTime,
    val createdBy: UUID,
    val type: DepositWithdrawType,
    val coop: String,
    val approvedAt: ZonedDateTime?,
    val amount: Long?,
    val documentResponse: DocumentResponse?,
    val txHash: String?,
    val declinedAt: ZonedDateTime?,
    val declinedComment: String?
) {
    constructor(deposit: Deposit) : this(
        deposit.id,
        deposit.ownerUuid,
        deposit.reference,
        deposit.approved,
        deposit.createdAt,
        deposit.createdBy,
        deposit.type,
        deposit.coop,
        deposit.approvedAt,
        deposit.amount,
        deposit.file?.let { DocumentResponse(it) },
        deposit.txHash,
        deposit.declined?.createdAt,
        deposit.declined?.comment
    )
}

data class DepositWithUserResponse(
    val deposit: DepositResponse,
    val user: UserControllerResponse?,
    val coop: String
) {
    constructor(deposit: Deposit, userResponse: UserResponse?) : this(
        DepositResponse(deposit),
        userResponse?.let { UserControllerResponse(it) },
        deposit.coop
    )
}

data class DepositWithUserListResponse(
    val deposits: List<DepositWithUserResponse>,
    val page: Int,
    val totalPages: Int
)

data class DepositWithProjectResponse(
    val deposit: DepositResponse,
    val project: ProjectControllerResponse?,
    val coop: String
) {
    constructor(deposit: Deposit, project: ProjectResponse?) : this(
        DepositResponse(deposit),
        project?.let { ProjectControllerResponse(it) },
        deposit.coop
    )
}

data class DepositWithProjectListResponse(
    val deposits: List<DepositWithProjectResponse>,
    val page: Int,
    val totalPages: Int
)
