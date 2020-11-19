package com.ampnet.walletservice.grpc

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.proto.ActivateWalletRequest
import com.ampnet.walletservice.proto.Empty
import com.ampnet.walletservice.proto.GetWalletsByHashRequest
import com.ampnet.walletservice.proto.GetWalletsByOwnerRequest
import com.ampnet.walletservice.proto.WalletResponse
import com.ampnet.walletservice.proto.WalletServiceGrpc
import com.ampnet.walletservice.proto.WalletsResponse
import com.ampnet.walletservice.service.WalletService
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class GrpcWalletServer(private val walletRepository: WalletRepository, private val walletService: WalletService) :
    WalletServiceGrpc.WalletServiceImplBase() {

    companion object : KLogging()

    override fun getWalletsByOwner(
        request: GetWalletsByOwnerRequest,
        responseObserver: StreamObserver<WalletsResponse>
    ) {
        logger.debug { "Received gRPC request: getWalletsByOwner = ${request.ownersUuidsList}" }
        val uuids = request.ownersUuidsList.mapNotNull {
            try {
                UUID.fromString(it)
            } catch (ex: IllegalArgumentException) {
                logger.warn(ex.message)
                null
            }
        }.toSet()
        val wallets = walletRepository.findByOwnerIn(uuids)
            .map { generateWalletResponseFromWallet(it) }
        logger.debug { "Wallets response: $wallets" }
        val response = WalletsResponse.newBuilder()
            .addAllWallets(wallets)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getWalletsByHash(request: GetWalletsByHashRequest, responseObserver: StreamObserver<WalletsResponse>) {
        logger.debug { "Received gRPC request: getWalletsByHash = ${request.hashesList}" }
        val wallets = walletRepository.findByHashes(request.hashesList)
            .map { generateWalletResponseFromWallet(it) }
        logger.debug { "Wallets response: $wallets" }
        val response = WalletsResponse.newBuilder()
            .addAllWallets(wallets)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun activateWallet(request: ActivateWalletRequest, responseObserver: StreamObserver<Empty>) {
        walletService.activateAdminWallet(request.address, request.coop, request.hash)
        responseObserver.onNext(Empty.newBuilder().build())
        responseObserver.onCompleted()
    }

    private fun generateWalletResponseFromWallet(wallet: Wallet): WalletResponse {
        val builder = WalletResponse.newBuilder()
            .setUuid(wallet.uuid.toString())
            .setOwner(wallet.owner.toString())
            .setActivationData(wallet.activationData)
            .setType(getWalletType(wallet.type))
            .setCurrency(wallet.currency.name)
            .setCoop(wallet.coop)
        wallet.hash?.let { builder.setHash(it) }
        return builder.build()
    }

    private fun getWalletType(type: WalletType): WalletResponse.Type =
        when (type) {
            WalletType.USER -> WalletResponse.Type.USER
            WalletType.ORG -> WalletResponse.Type.ORGANIZATION
            WalletType.PROJECT -> WalletResponse.Type.PROJECT
        }
}
