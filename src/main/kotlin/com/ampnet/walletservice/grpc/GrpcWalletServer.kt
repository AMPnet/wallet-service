package com.ampnet.walletservice.grpc

import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.proto.GetWalletsByHashRequest
import com.ampnet.walletservice.proto.GetWalletsByOwnerRequest
import com.ampnet.walletservice.proto.WalletResponse
import com.ampnet.walletservice.proto.WalletServiceGrpc
import com.ampnet.walletservice.proto.WalletsResponse
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class GrpcWalletServer(val walletRepository: WalletRepository) : WalletServiceGrpc.WalletServiceImplBase() {

    companion object : KLogging()

    override fun getWalletsByOwner(request: GetWalletsByOwnerRequest, responseObserver: StreamObserver<WalletsResponse>) {
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
        val hashes = request.hashesList.filter { it.isNotEmpty() }
        val wallets = walletRepository.findByHashes(hashes)
            .map { generateWalletResponseFromWallet(it) }
        logger.debug { "Wallets response: $wallets" }
        val response = WalletsResponse.newBuilder()
            .addAllWallets(wallets)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun generateWalletResponseFromWallet(wallet: Wallet): WalletResponse {
        val builder = WalletResponse.newBuilder()
            .setUuid(wallet.uuid.toString())
            .setOwner(wallet.owner.toString())
            .setActivationData(wallet.activationData)
            .setType(getWalletType(wallet.type))
            .setCurrency(wallet.currency.name)
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
