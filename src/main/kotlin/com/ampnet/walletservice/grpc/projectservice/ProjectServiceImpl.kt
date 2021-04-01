package com.ampnet.walletservice.grpc.projectservice

import com.ampnet.projectservice.proto.GetByUuid
import com.ampnet.projectservice.proto.GetByUuids
import com.ampnet.projectservice.proto.OrganizationMembershipResponse
import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.projectservice.proto.ProjectServiceGrpc
import com.ampnet.walletservice.config.ApplicationProperties
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class ProjectServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : ProjectService {

    companion object : KLogging()

    private val serviceBlockingStub: ProjectServiceGrpc.ProjectServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("project-service")
        ProjectServiceGrpc.newBlockingStub(channel)
    }

    @Throws(ResourceNotFoundException::class)
    override fun getOrganization(uuid: UUID): OrganizationResponse {
        return getOrganizations(listOf(uuid)).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization: $uuid")
    }

    @Throws(ResourceNotFoundException::class)
    override fun getProject(uuid: UUID): ProjectServiceResponse {
        return getProjects(listOf(uuid)).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $uuid")
    }

    override fun getOrganizations(uuids: Iterable<UUID>): List<OrganizationResponse> {
        if (uuids.none()) return emptyList()
        logger.debug { "Fetching organizations: $uuids" }
        try {
            val request = GetByUuids.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
            val response = serviceWithTimeout().getOrganizations(request).organizationsList
            logger.debug { "Fetched organizations: ${response.map { it.uuid }}" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_PROJECT, "Failed to fetch organizations. ${ex.localizedMessage}")
        }
    }

    override fun getProjects(uuids: Iterable<UUID>): List<ProjectServiceResponse> {
        if (uuids.none()) return emptyList()
        logger.debug { "Fetching projects: $uuids" }
        try {
            val request = GetByUuids.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
            val response = serviceWithTimeout().getProjects(request).projectsList
            logger.debug { "Fetched projects: ${response.map { it.uuid }}" }
            return response.map { ProjectServiceResponse(it) }
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_PROJECT, "Failed to fetch projects. ${ex.localizedMessage}")
        }
    }

    override fun getOrganizationMembersForProject(projectUuid: UUID): List<OrganizationMembershipResponse> {
        logger.debug { "Fetching organization members for project: $projectUuid" }
        try {
            val request = GetByUuid.newBuilder()
                .setProjectUuid(projectUuid.toString())
                .build()
            val response = serviceWithTimeout().getOrganizationMembersForProject(request).membershipsList
            logger.debug { "Fetched organization members: ${response.map { it.userUuid }}" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_PROJECT, "Failed to fetch organization members. ${ex.localizedMessage}")
        }
    }

    private fun serviceWithTimeout() = serviceBlockingStub
        .withDeadlineAfter(applicationProperties.grpc.projectServiceTimeout, TimeUnit.MILLISECONDS)
}
