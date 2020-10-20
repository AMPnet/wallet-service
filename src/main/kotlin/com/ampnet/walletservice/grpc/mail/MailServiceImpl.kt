package com.ampnet.walletservice.grpc.mail

import com.ampnet.mailservice.proto.ActivatedWalletRequest
import com.ampnet.mailservice.proto.DepositInfoRequest
import com.ampnet.mailservice.proto.Empty
import com.ampnet.mailservice.proto.MailServiceGrpc
import com.ampnet.mailservice.proto.WalletType
import com.ampnet.mailservice.proto.WalletTypeRequest
import com.ampnet.mailservice.proto.WithdrawInfoRequest
import com.ampnet.mailservice.proto.WithdrawRequest
import com.ampnet.walletservice.config.ApplicationProperties
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class MailServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : MailService {

    companion object : KLogging()

    private val mailServiceStub: MailServiceGrpc.MailServiceStub by lazy {
        val channel = grpcChannelFactory.createChannel("mail-service")
        MailServiceGrpc.newStub(channel)
    }

    override fun sendDepositInfo(user: UUID, minted: Boolean) {
        logger.debug { "Sending deposit info mail" }
        try {
            val request = DepositInfoRequest.newBuilder()
                .setUser(user.toString())
                .setMinted(minted)
                .build()
            serviceWithTimeout()?.sendDepositInfo(request, createSteamObserver("deposit info mail to: $user"))
        } catch (ex: StatusRuntimeException) {
            logger.warn("Failed to send deposit info mail.", ex)
        }
    }

    override fun sendWithdrawRequest(user: UUID, amount: Long) {
        logger.debug { "Sending withdraw request mail" }
        try {
            val request = WithdrawRequest.newBuilder()
                .setUser(user.toString())
                .setAmount(amount)
                .build()
            serviceWithTimeout()?.sendWithdrawRequest(request, createSteamObserver("withdraw request mail to: $user"))
        } catch (ex: StatusRuntimeException) {
            logger.warn("Failed to send withdraw request mail.", ex)
        }
    }

    override fun sendWithdrawInfo(user: UUID, burned: Boolean) {
        logger.debug { "Sending withdraw info mail" }
        try {
            val request = WithdrawInfoRequest.newBuilder()
                .setUser(user.toString())
                .setBurned(burned)
                .build()
            serviceWithTimeout()?.sendWithdrawInfo(request, createSteamObserver("withdraw info mail to: $user"))
        } catch (ex: StatusRuntimeException) {
            logger.warn("Failed to send withdraw info mail.", ex)
        }
    }

    override fun sendNewWalletMail(walletType: WalletType) {
        logger.debug { "Sending new $walletType wallet mail" }
        try {
            val request = WalletTypeRequest.newBuilder()
                .setType(walletType)
                .build()
            serviceWithTimeout()?.sendNewWalletMail(request, createSteamObserver("new $walletType wallet mail"))
        } catch (ex: StatusRuntimeException) {
            logger.warn("Failed to send new $walletType wallet mail.", ex)
        }
    }

    override fun sendWalletActivated(walletType: WalletType, owner: String) {
        logger.debug { "Sending ${walletType.name} wallet approved mail" }
        try {
            val request = ActivatedWalletRequest.newBuilder()
                .setType(walletType)
                .setOwner(owner)
                .build()
            serviceWithTimeout()?.sendWalletActivated(request, createSteamObserver("wallet is approved for owner $owner"))
        } catch (ex: StatusRuntimeException) {
            logger.warn("Failed to send ${walletType.name} wallet approved mail.", ex)
        }
    }

    private fun serviceWithTimeout(): MailServiceGrpc.MailServiceStub? {
        return if (applicationProperties.mail.sendNotification) {
            mailServiceStub.withDeadlineAfter(applicationProperties.grpc.mailServiceTimeout, TimeUnit.MILLISECONDS)
        } else {
            logger.info { "Sending email disabled" }
            null
        }
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
