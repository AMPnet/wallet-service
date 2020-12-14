package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.service.WalletService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PublicController(private val walletService: WalletService, private val blockchainService: BlockchainService) {

    companion object : KLogging()

    @GetMapping("/public/wallet/project/{projectUuid}")
    fun getProjectWallet(@PathVariable projectUuid: UUID): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to get wallet for project: $projectUuid" }
        walletService.getWallet(projectUuid)?.let { wallet ->
            val balance = wallet.hash?.let { blockchainService.getBalance(it) }
            return ResponseEntity.ok(WalletResponse(wallet, balance))
        }
        return ResponseEntity.notFound().build()
    }
}
