package com.ampnet.walletservice.grpc.userservice

import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.userservice.proto.UserServiceGrpc
import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.GrpcException
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory
) : UserService {

    companion object : KLogging()

    private val serviceBlockingStub: UserServiceGrpc.UserServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("user-service")
        UserServiceGrpc.newBlockingStub(channel)
    }

    override fun getUsers(uuids: Iterable<UUID>): List<UserResponse> {
        logger.debug { "Fetching users: $uuids" }
        try {
            val request = GetUsersRequest.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
            val response = serviceBlockingStub.getUsers(request).usersList
            logger.debug { "Fetched users: $response" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_USER, "Failed to fetch users. ${ex.localizedMessage}")
        }
    }
}
