package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.project.proto.ProjectResponse
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class ProjectControllerResponse(
    val uuid: String,
    val name: String,
    val endDate: ZonedDateTime,
    val expectedFunding: Long,
    val currency: String,
    val minPerUser: Long,
    val maxPerUser: Long,
    val active: Boolean
) {
    constructor(project: ProjectResponse) : this(
        project.uuid,
        project.name,
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(project.endDate), ZoneId.systemDefault()),
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.active
    )
}
