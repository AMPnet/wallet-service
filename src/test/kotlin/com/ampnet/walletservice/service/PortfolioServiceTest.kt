package com.ampnet.walletservice.service

import com.ampnet.projectservice.proto.OrganizationMembershipResponse
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.service.impl.PortfolioServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.UUID

class PortfolioServiceTest : JpaServiceTestBase() {

    private val portfolioService: PortfolioService by lazy {
        PortfolioServiceImpl(
            walletRepository, mockedBlockchainService,
            mockedProjectService, mockedUserService, mockedWalletService
        )
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllWallets()
        testContext = TestContext()
    }

    @Test
    fun mustThrowExceptionIfUserNotMemberOfOrganization() {
        suppose("Project service will return a list organization members") {
            testContext.organizationMembers = listOf(
                createOrganizationMembership(UUID.randomUUID()), createOrganizationMembership(UUID.randomUUID())
            )
            Mockito.`when`(mockedProjectService.getOrganizationMembers(projectUuid))
                .thenReturn(testContext.organizationMembers)
        }

        verify("Service will throw exception user not member of organization") {
            val exception = assertThrows<InvalidRequestException> {
                portfolioService.getProjectTransactions(projectUuid, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MEM_MISSING)
        }
    }

    private class TestContext {
        lateinit var organizationMembers: List<OrganizationMembershipResponse>
    }
}
