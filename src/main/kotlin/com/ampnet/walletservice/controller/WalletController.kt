package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WalletCreateRequest
import com.ampnet.walletservice.controller.pojo.response.PairWalletResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.service.WalletService
import java.util.UUID
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class WalletController(
    private val walletService: WalletService
) {

    companion object : KLogging()

    /* User Wallet */
    @GetMapping("/wallet/pair/{code}")
    fun getPairWalletCode(@PathVariable code: String): ResponseEntity<PairWalletResponse> {
        logger.debug { "Received request getPairWalletCode" }
        walletService.getPairWalletCode(code)?.let {
            return ResponseEntity.ok(PairWalletResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/wallet/pair")
    fun generatePairWalletCode(@RequestBody request: WalletCreateRequest): ResponseEntity<PairWalletResponse> {
        logger.debug { "Received request to pair wallet: $request" }
        val pairWalletCode = walletService.generatePairWalletCode(request.publicKey)
        return ResponseEntity.ok(PairWalletResponse(pairWalletCode))
    }

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request for Wallet from user: ${userPrincipal.uuid}" }
        walletService.getWallet(userPrincipal.uuid)?.let {
            val balance = walletService.getWalletBalance(it)
            val response = WalletResponse(it, balance)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/wallet")
    fun createWallet(@RequestBody request: WalletCreateRequest): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request from user: ${userPrincipal.uuid} to create wallet: $request" }
        val wallet = walletService.createUserWallet(userPrincipal.uuid, request.publicKey)
        val response = WalletResponse(wallet)
        return ResponseEntity.ok(response)
    }

    /* Project Wallet */
    @GetMapping("/wallet/project/{projectUuid}/transaction")
    fun getTransactionToCreateProjectWallet(@PathVariable projectUuid: UUID): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create a Wallet for project: $projectUuid by user: ${userPrincipal.uuid}" }
        val transaction =
            walletService.generateTransactionToCreateProjectWallet(projectUuid, userPrincipal.uuid)
        val response = TransactionResponse(transaction)
        return ResponseEntity.ok(response)
    }

    /* Organization Wallet */
    @GetMapping("/wallet/organization/{organizationUuid}")
    fun getOrganizationWallet(@PathVariable organizationUuid: UUID): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to get Wallet for organization $organizationUuid by user: ${userPrincipal.email}"
        }
        walletService.getWallet(organizationUuid)?.let {
            val balance = walletService.getWalletBalance(it)
            return ResponseEntity.ok(WalletResponse(it, balance))
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/wallet/organization/{organizationUuid}/transaction")
    fun getTransactionToCreateOrganizationWallet(
        @PathVariable organizationUuid: UUID
    ): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to create Wallet for Organization: $organizationUuid by user: ${userPrincipal.email}"
        }
        val transaction = walletService
                .generateTransactionToCreateOrganizationWallet(organizationUuid, userPrincipal.uuid)
        val response = TransactionResponse(transaction)
        return ResponseEntity.ok(response)
    }
}
