package com.ampnet.walletservice.grpc

import com.ampnet.walletservice.TestBase
import com.ampnet.walletservice.enums.Currency
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.proto.GetWalletsByOwnerRequest
import com.ampnet.walletservice.proto.WalletResponse
import com.ampnet.walletservice.proto.WalletsResponse
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

class GrpcWalletServerTest : TestBase() {

    private val mockedWalletRepository = Mockito.mock(WalletRepository::class.java)

    private lateinit var grpcServer: GrpcWalletServer
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        Mockito.reset(mockedWalletRepository)
        grpcServer = GrpcWalletServer(mockedWalletRepository)
        testContext = TestContext()
    }

    @Test
    fun mustReturnRequestedWallets() {
        suppose("Wallets exist") {
            testContext.uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
            testContext.wallets = createListOfWallets(testContext.uuids)
            Mockito.`when`(mockedWalletRepository.findByOwnerIn(testContext.uuids.toSet())).thenReturn(testContext.wallets)
        }

        verify("Grpc service will return wallets") {
            val request = GetWalletsByOwnerRequest.newBuilder()
                .addAllOwnersUuids(testContext.uuids.map { it.toString() })
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<WalletsResponse>
            grpcServer.getWallets(request, streamObserver)
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
                .build()
        }

    private fun createListOfWallets(uuids: List<UUID>): List<Wallet> =
        uuids.map {
            Wallet(it, "activation-data", WalletType.USER, Currency.EUR)
        }

    private class TestContext {
        lateinit var uuids: List<UUID>
        lateinit var wallets: List<Wallet>
    }
}
