package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.walletservice.controller.ControllerUtils
import java.time.ZonedDateTime

data class OrganizationControllerResponse(
    val uuid: String,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean
) {
    constructor(organization: OrganizationResponse) : this(
        organization.uuid,
        organization.name,
        ControllerUtils.epochMilliToZonedDateTime(organization.createdAt),
        organization.approved
    )
}
