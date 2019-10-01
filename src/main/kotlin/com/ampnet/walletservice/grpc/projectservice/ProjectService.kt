package com.ampnet.walletservice.grpc.projectservice

import com.ampnet.projectservice.proto.OrganizationResponse
import com.ampnet.projectservice.proto.ProjectResponse
import java.util.UUID

interface ProjectService {
    fun getOrganization(uuid: UUID): OrganizationResponse
    fun getProject(uuid: UUID): ProjectResponse
    fun getOrganizations(uuids: Iterable<UUID>): List<OrganizationResponse>
    fun getProjects(uuids: Iterable<UUID>): List<ProjectResponse>
}
