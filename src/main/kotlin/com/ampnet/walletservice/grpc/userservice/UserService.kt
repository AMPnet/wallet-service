package com.ampnet.walletservice.grpc.userservice

import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.walletservice.service.pojo.response.UserServiceResponse
import java.util.UUID

interface UserService {
    fun getUsers(uuids: Set<UUID>): List<UserServiceResponse>
    fun setUserRole(uuid: UUID, role: SetRoleRequest.Role, coop: String): UserServiceResponse
}
