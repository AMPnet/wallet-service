package com.ampnet.walletservice

import com.ampnet.project.proto.OrganizationResponse
import com.ampnet.project.proto.ProjectResponse
import org.springframework.test.context.ActiveProfiles
import java.time.ZonedDateTime
import java.util.UUID

@ActiveProfiles("test")
abstract class TestBase {

    protected fun suppose(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun verify(@Suppress("UNUSED_PARAMETER") description: String, function: () -> Unit) {
        function.invoke()
    }

    protected fun getOrganizationResponse(organization: UUID, user: UUID, name: String = "Name"): OrganizationResponse =
        OrganizationResponse.newBuilder()
            .setUuid(organization.toString())
            .setApproved(true)
            .setCreatedAt(ZonedDateTime.now().minusDays(1).toInstant().epochSecond.toString())
            .setCreatedByUser(user.toString())
            .setName(name)
            .build()

    protected fun getProjectResponse(
        project: UUID,
        user: UUID,
        organization: UUID,
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        name: String = "name",
        active: Boolean = true,
        expectedFunding: Long = 10000000
    ): ProjectResponse =
        ProjectResponse.newBuilder()
            .setUuid(project.toString())
            .setName(name)
            .setActive(active)
            .setCreatedByUser(user.toString())
            .setCurrency("EUR")
            .setEndDate(endDate.toInstant().toEpochMilli())
            .setMinPerUser(100)
            .setMaxPerUser(100000)
            .setExpectedFunding(expectedFunding)
            .setOrganizationUuid(organization.toString())
            .build()
}
