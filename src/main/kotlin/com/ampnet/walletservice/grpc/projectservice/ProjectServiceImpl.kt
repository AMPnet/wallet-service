package com.ampnet.walletservice.grpc.projectservice

import com.ampnet.projectservice.proto.GetByUuids
import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.projectservice.proto.ProjectServiceGrpc
import com.ampnet.walletservice.config.ApplicationProperties
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import io.grpc.StatusRuntimeException
import java.util.UUID
import java.util.concurrent.TimeUnit
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class ProjectServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : ProjectService {

    companion object : KLogging()

    private val serviceBlockingStub: ProjectServiceGrpc.ProjectServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("project-service")
        ProjectServiceGrpc.newBlockingStub(channel)
            .withDeadlineAfter(applicationProperties.grpc.projectServiceTimeout, TimeUnit.MILLISECONDS)
    }

    @Throws(ResourceNotFoundException::class)
    override fun getOrganization(uuid: UUID): OrganizationResponse {
        return getOrganizations(listOf(uuid)).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization: $uuid")
    }

    @Throws(ResourceNotFoundException::class)
    override fun getProject(uuid: UUID): ProjectResponse {
        return getProjects(listOf(uuid)).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $uuid")
    }

    override fun getOrganizations(uuids: Iterable<UUID>): List<OrganizationResponse> {
        logger.debug { "Fetching organizations: $uuids" }
        try {
            val request = GetByUuids.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
            val response = serviceBlockingStub.getOrganizations(request).organizationsList
            logger.debug { "Fetched organizations: $response" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_PROJECT, "Failed to fetch organizations. ${ex.localizedMessage}")
        }
    }

    override fun getProjects(uuids: Iterable<UUID>): List<ProjectResponse> {
        logger.debug { "Fetching projects: $uuids" }
        try {
            val request = GetByUuids.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
            val response = serviceBlockingStub.getProjects(request).projectsList
            logger.debug { "Fetched projects: $response" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_PROJECT, "Failed to fetch projects. ${ex.localizedMessage}")
        }
    }
}
