package com.ampnet.walletservice.grpc.userservice

import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.userservice.proto.UserServiceGrpc
import com.ampnet.walletservice.config.ApplicationProperties
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import io.grpc.StatusRuntimeException
import java.util.UUID
import java.util.concurrent.TimeUnit
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class UserServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : UserService {

    companion object : KLogging()

    private val serviceBlockingStub: UserServiceGrpc.UserServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("user-service")
        UserServiceGrpc.newBlockingStub(channel)
    }

    override fun getUsers(uuids: Set<UUID>): List<UserResponse> {
        logger.debug { "Fetching users: $uuids" }
        try {
            val request = GetUsersRequest.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
            val response = serviceWithTimeout().getUsers(request).usersList
            logger.debug { "Fetched users: $response" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_USER, "Failed to fetch users. ${ex.localizedMessage}")
        }
    }

    override fun setUserRole(uuid: UUID, role: SetRoleRequest.Role): UserResponse {
        logger.info { "Received request to change user: $uuid role to: ${role.name}" }
        try {
            val request = SetRoleRequest.newBuilder()
                .setUuid(uuid.toString())
                .setRole(role)
                .build()
            val response = serviceWithTimeout().setUserRole(request)

            logger.info { "Successfully change role for user: ${response.uuid}" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_USER, "Failed to change role for user: $uuid")
        }
    }

    private fun serviceWithTimeout() = serviceBlockingStub
        .withDeadlineAfter(applicationProperties.grpc.userServiceTimeout, TimeUnit.MILLISECONDS)
}
