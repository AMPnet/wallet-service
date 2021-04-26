package com.ampnet.walletservice.service.pojo.response

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.walletservice.controller.ControllerUtils
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectServiceResponse(
    val uuid: UUID,
    val name: String,
    val description: String,
    val startDate: ZonedDateTime?,
    val endDate: ZonedDateTime?,
    val expectedFunding: Long?,
    val currency: String?,
    val minPerUser: Long?,
    val maxPerUser: Long?,
    val active: Boolean,
    val imageUrl: String,
    val createByUuid: UUID,
    val organizationUuid: UUID
) {
    constructor(project: ProjectResponse) : this(
        UUID.fromString(project.uuid),
        project.name,
        project.description,
        if (project.startDate == 0L) null else ControllerUtils.epochMilliToZonedDateTime(project.startDate),
        if (project.endDate == 0L) null else ControllerUtils.epochMilliToZonedDateTime(project.endDate),
        project.expectedFunding,
        project.currency,
        if (project.minPerUser == 0L) null else project.minPerUser,
        if (project.maxPerUser == 0L) null else project.maxPerUser,
        project.active,
        project.imageUrl,
        UUID.fromString(project.createdByUser),
        UUID.fromString(project.organizationUuid)
    )
}
