package com.ampnet.walletservice.grpc.mail

import com.ampnet.mailservice.proto.DepositInfoRequest
import com.ampnet.mailservice.proto.DepositRequest
import com.ampnet.mailservice.proto.Empty
import com.ampnet.mailservice.proto.MailServiceGrpc
import com.ampnet.mailservice.proto.WithdrawInfoRequest
import com.ampnet.mailservice.proto.WithdrawRequest
import com.ampnet.walletservice.config.ApplicationProperties
import io.grpc.stub.StreamObserver
import java.util.UUID
import java.util.concurrent.TimeUnit
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class MailServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : MailService {

    companion object : KLogging()

    private val mailServiceStub: MailServiceGrpc.MailServiceStub by lazy {
        val channel = grpcChannelFactory.createChannel("mail-service")
        MailServiceGrpc.newStub(channel)
            .withDeadlineAfter(applicationProperties.grpc.mailServiceTimeout, TimeUnit.MILLISECONDS)
    }

    override fun sendDepositRequest(user: UUID, amount: Long) {
        val request = DepositRequest.newBuilder()
                .setUser(user.toString())
                .setAmount(amount)
                .build()

        mailServiceStub.sendDepositRequest(request, createSteamObserver("deposit request mail to: $user"))
    }

    override fun sendDepositInfo(user: UUID, minted: Boolean) {
        val request = DepositInfoRequest.newBuilder()
                .setUser(user.toString())
                .setMinted(minted)
                .build()

        mailServiceStub.sendDepositInfo(request, createSteamObserver("deposit info mail to: $user"))
    }

    override fun sendWithdrawRequest(user: UUID, amount: Long) {
        val request = WithdrawRequest.newBuilder()
                .setUser(user.toString())
                .setAmount(amount)
                .build()

        mailServiceStub.sendWithdrawRequest(request, createSteamObserver("withdraw request mail to: $user"))
    }

    override fun sendWithdrawInfo(user: UUID, burned: Boolean) {
        val request = WithdrawInfoRequest.newBuilder()
                .setUser(user.toString())
                .setBurned(burned)
                .build()

        mailServiceStub.sendWithdrawInfo(request, createSteamObserver("withdraw info mail to: $user"))
    }

    private fun createSteamObserver(message: String) =
        object : StreamObserver<Empty> {
            override fun onNext(value: Empty?) {
                logger.debug { "Successfully sent $message" }
            }

            override fun onError(t: Throwable?) {
                logger.warn { "Failed to sent $message. ${t?.message}" }
            }

            override fun onCompleted() {
                // successfully sent
            }
        }
}
