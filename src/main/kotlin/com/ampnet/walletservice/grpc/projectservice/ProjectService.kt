package com.ampnet.walletservice.grpc.projectservice

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import java.util.UUID

interface ProjectService {
    fun getOrganization(uuid: UUID): OrganizationResponse
    fun getProject(uuid: UUID): ProjectServiceResponse
    fun getOrganizations(uuids: Iterable<UUID>): List<OrganizationResponse>
    fun getProjects(uuids: Iterable<UUID>): List<ProjectServiceResponse>
}
