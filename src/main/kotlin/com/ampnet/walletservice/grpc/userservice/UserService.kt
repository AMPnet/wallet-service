package com.ampnet.walletservice.grpc.userservice

import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.userservice.proto.UserResponse
import java.util.UUID

interface UserService {
    fun getUsers(uuids: Set<UUID>): List<UserResponse>
    fun setUserRole(uuid: UUID, role: SetRoleRequest.Role): UserResponse
}
