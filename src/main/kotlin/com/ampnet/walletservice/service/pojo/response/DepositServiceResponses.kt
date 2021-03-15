package com.ampnet.walletservice.service.pojo.response

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
    val documentResponse: DocumentServiceResponse?,
    val coop: String,
    val userConfirmation: Boolean
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
        if (withDocument) deposit.file?.let { DocumentServiceResponse(it) } else null,
        deposit.coop,
        deposit.userConfirmation
    )
}

data class DepositWithDataServiceResponse(
    val deposit: DepositServiceResponse,
    val user: UserServiceResponse?,
    val project: ProjectServiceResponse?
) {
    constructor(
        deposit: Deposit,
        userResponse: UserServiceResponse?,
        projectResponse: ProjectServiceResponse?,
        withDocument: Boolean = false
    ) : this(
        DepositServiceResponse(deposit, withDocument),
        userResponse,
        projectResponse
    )
}

data class DepositListServiceResponse(
    val deposits: List<DepositWithDataServiceResponse>,
    val page: Int,
    val totalPages: Int
)
