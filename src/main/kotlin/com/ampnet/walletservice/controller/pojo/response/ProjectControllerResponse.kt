package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.walletservice.controller.ControllerUtils
import java.time.ZonedDateTime

data class ProjectControllerResponse(
    val uuid: String,
    val name: String,
    val description: String,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val expectedFunding: Long,
    val currency: String,
    val minPerUser: Long,
    val maxPerUser: Long,
    val active: Boolean,
    val imageUrl: String
) {
    constructor(project: ProjectResponse) : this(
        project.uuid,
        project.name,
        project.description,
        ControllerUtils.epochMilliToZonedDateTime(project.startDate),
        ControllerUtils.epochMilliToZonedDateTime(project.endDate),
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.active,
        project.imageUrl
    )
}
