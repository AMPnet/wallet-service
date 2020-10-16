package com.ampnet.walletservice

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInfoResponse
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
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
            .setCreatedAt(ZonedDateTime.now().minusDays(1).toInstant().epochSecond)
            .setCreatedByUser(user.toString())
            .setName(name)
            .build()

    protected fun getProjectResponse(
        project: UUID,
        user: UUID = UUID.randomUUID(),
        organization: UUID = UUID.randomUUID(),
        currency: String = "EUR",
        name: String = "project",
        imageUrl: String = "image_url",
        active: Boolean = true,
        description: String = "Description",
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        expectedFunding: Long = 100000000L
    ): ProjectServiceResponse = ProjectServiceResponse(
        project, name, description,
        ZonedDateTime.now().minusDays(1), endDate,
        expectedFunding, currency, 100, 100000, active, imageUrl, user, organization
    )

    protected fun getProjectInfoResponse(
        walletHash: String,
        balance: Long,
        investmentCap: Long = 10_000_000_000_00,
        minPerUser: Long = 100_00,
        maxPerUser: Long = 10_000_000_00,
        endsAt: Long = ZonedDateTime.now().plusDays(30).toEpochSecond()
    ): ProjectInfoResponse =
        ProjectInfoResponse(walletHash, balance, investmentCap, minPerUser, maxPerUser, endsAt, false)

    protected fun createProjectResponse(
        project: UUID,
        createBy: UUID
    ): ProjectServiceResponse = ProjectServiceResponse(
        project, "Project name", "Description", ZonedDateTime.now().minusDays(1), ZonedDateTime.now().plusDays(30),
        10_000_000_00, Currency.EUR.name, 1_00, 10_000_00, true, "image-url", createBy, UUID.randomUUID()
    )
}
