package com.ampnet.walletservice.grpc

import com.ampnet.walletservice.TestBase
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.proto.GetWalletsByHashRequest
import com.ampnet.walletservice.proto.GetWalletsByOwnerRequest
import com.ampnet.walletservice.proto.WalletResponse
import com.ampnet.walletservice.proto.WalletsResponse
import com.ampnet.walletservice.service.CooperativeWalletService
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

class GrpcWalletServerTest : TestBase() {

    private val coop = "ampnet"

    private val mockedWalletRepository = Mockito.mock(WalletRepository::class.java)
    private val mockedCooperativeWalletService = Mockito.mock(CooperativeWalletService::class.java)

    private lateinit var grpcServer: GrpcWalletServer
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        Mockito.reset(mockedWalletRepository)
        grpcServer = GrpcWalletServer(mockedWalletRepository, mockedCooperativeWalletService)
        testContext = TestContext()
    }

    @Test
    fun mustReturnRequestedWalletsByOwners() {
        suppose("Wallets exist") {
            testContext.uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
            testContext.wallets = createListOfWalletsWithHashes(testContext.uuids)
            Mockito.`when`(mockedWalletRepository.findByOwnerIn(testContext.uuids.toSet())).thenReturn(testContext.wallets)
        }

        verify("Grpc service will return wallets") {
            val request = GetWalletsByOwnerRequest.newBuilder()
                .addAllOwnersUuids(testContext.uuids.map { it.toString() })
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<WalletsResponse>
            grpcServer.getWalletsByOwner(request, streamObserver)
            val walletsResponse = generateWalletsResponse(testContext.wallets)
            val response = WalletsResponse.newBuilder().addAllWallets(walletsResponse).build()
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    @Test
    fun mustReturnRequestedWalletsByHashes() {
        suppose("Wallets exist") {
            testContext.uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
            testContext.hashes = listOf("hash-1", "hash-2")
            testContext.wallets = createListOfWalletsWithHashes(testContext.uuids, testContext.hashes)
            Mockito.`when`(mockedWalletRepository.findByHashes(testContext.hashes)).thenReturn(testContext.wallets)
        }

        verify("Grpc service will return wallets") {
            val request = GetWalletsByHashRequest.newBuilder()
                .addAllHashes(testContext.hashes)
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<WalletsResponse>
            grpcServer.getWalletsByHash(request, streamObserver)
            val walletsResponse = generateWalletsResponse(testContext.wallets)
            val response = WalletsResponse.newBuilder().addAllWallets(walletsResponse).build()
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    private fun generateWalletsResponse(wallets: List<Wallet>): List<WalletResponse> =
        wallets.map {
            WalletResponse.newBuilder()
                .setUuid(it.uuid.toString())
                .setOwner(it.owner.toString())
                .setActivationData("activation-data")
                .setType(WalletResponse.Type.USER)
                .setCurrency(Currency.EUR.name)
                .setHash(it.hash)
                .setCoop(it.coop)
                .build()
        }

    private fun createListOfWalletsWithHashes(
        uuids: List<UUID>,
        hashes: List<String> = mutableListOf()
    ): List<Wallet> {
        val uuidsWithHashes = uuids.zip(hashes).toMap()
        return uuidsWithHashes.map {
            val wallet = Wallet(it.key, "activation-data", WalletType.USER, Currency.EUR, coop, "alias")
            wallet.hash = it.value
            wallet
        }
    }

    private class TestContext {
        lateinit var uuids: List<UUID>
        lateinit var wallets: List<Wallet>
        lateinit var hashes: List<String>
    }
}
