package com.ampnet.walletservice.grpc

import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.DepositRepository
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.proto.CoopRequest
import com.ampnet.walletservice.proto.GetWalletsByHashRequest
import com.ampnet.walletservice.proto.GetWalletsByOwnerRequest
import com.ampnet.walletservice.proto.OwnersResponse
import com.ampnet.walletservice.proto.WalletResponse
import com.ampnet.walletservice.proto.WalletServiceGrpc
import com.ampnet.walletservice.proto.WalletsResponse
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.data.domain.Pageable
import java.util.UUID

@GrpcService
class GrpcWalletServer(
    private val walletRepository: WalletRepository,
    private val depositRepository: DepositRepository
) : WalletServiceGrpc.WalletServiceImplBase() {

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
        logger.debug { "Wallets response: ${wallets.size}" }
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
        logger.debug { "Wallets response: ${wallets.size}" }
        val response = WalletsResponse.newBuilder()
            .addAllWallets(wallets)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getOwnersWithDeposit(request: CoopRequest, responseObserver: StreamObserver<OwnersResponse>) {
        logger.debug { "Received gRPC request: getOwnersWithDeposit for coop = ${request.coop}" }
        val owners = depositRepository
            .findAllApprovedWithFile(request.coop, DepositWithdrawType.USER, Pageable.unpaged())
            .map { it.ownerUuid.toString() }
        val response = OwnersResponse.newBuilder()
            .addAllOwnersUuids(owners)
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
