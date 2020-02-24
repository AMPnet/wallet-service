package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.service.DepositService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DepositController(
    private val depositService: DepositService
) {

    companion object : KLogging()

    @PostMapping("/deposit")
    fun createDeposit(@RequestBody request: AmountRequest): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create deposit" }
        val deposit = depositService.create(userPrincipal.uuid, request.amount)
        return ResponseEntity.ok(DepositResponse(deposit))
    }

    @GetMapping("/deposit")
    fun getPendingDeposit(): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get pending deposit by user: ${userPrincipal.uuid}" }
        depositService.getPendingForUser(userPrincipal.uuid)?.let {
            return ResponseEntity.ok(DepositResponse(it))
        }
        return ResponseEntity.notFound().build()
    }
}
