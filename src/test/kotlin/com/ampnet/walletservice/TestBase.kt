package com.ampnet.walletservice

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.grpc.blockchain.pojo.ProjectInfoResponse
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.test.context.ActiveProfiles

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
            .setDescription("description")
            .setImageUrl("image-url")
            .build()

    protected fun getProjectInfoResponse(
        walletHash: String,
        balance: Long,
        investmentCap: Long = 10_000_000_000_00,
        minPerUser: Long = 100_00,
        maxPerUser: Long = 10_000_000_00,
        endsAt: Long = ZonedDateTime.now().plusDays(30).toEpochSecond()
    ): ProjectInfoResponse =
        ProjectInfoResponse(walletHash, balance, investmentCap, minPerUser, maxPerUser, endsAt, false)

    protected fun createProjectResponse(project: UUID, createBy: UUID): ProjectResponse =
        ProjectResponse.newBuilder()
            .setUuid(project.toString())
            .setCreatedByUser(createBy.toString())
            .setActive(true)
            .setName("Project name")
            .setCurrency(Currency.EUR.name)
            .setDescription("Description")
            .setStartDate(ZonedDateTime.now().minusDays(1).toEpochSecond())
            .setEndDate(ZonedDateTime.now().plusDays(30).toEpochSecond())
            .setExpectedFunding(10_000_000_00)
            .setImageUrl("image-url")
            .setMaxPerUser(10_000_00)
            .setMinPerUser(1_00)
            .setOrganizationUuid(UUID.randomUUID().toString())
            .build()
}
