package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.service.DepositService
import com.ampnet.walletservice.service.pojo.DepositCreateServiceRequest
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DepositController(
    private val depositService: DepositService
) {

    companion object : KLogging()

    @PostMapping("/deposit")
    fun createDeposit(@RequestBody request: AmountRequest): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create deposit" }
        val serviceRequest = DepositCreateServiceRequest(
            userPrincipal.uuid, userPrincipal, request.amount, DepositWithdrawType.USER
        )
        val deposit = depositService.create(serviceRequest)
        return ResponseEntity.ok(DepositResponse(deposit))
    }

    @DeleteMapping("/deposit/{id}")
    fun deleteDeposit(@PathVariable("id") id: Int): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to delete deposit: $id by user: ${userPrincipal.uuid}" }
        depositService.delete(id, userPrincipal.uuid)
        return ResponseEntity.ok().build()
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

    @PostMapping("/deposit/project/{projectUuid}")
    fun createProjectDeposit(
        @PathVariable projectUuid: UUID,
        @RequestBody request: AmountRequest
    ): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create deposit" }
        val serviceRequest = DepositCreateServiceRequest(
            projectUuid, userPrincipal, request.amount, DepositWithdrawType.PROJECT
        )
        val deposit = depositService.create(serviceRequest)
        return ResponseEntity.ok(DepositResponse(deposit))
    }
}
