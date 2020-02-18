package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawResponse
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.service.WithdrawService
import com.ampnet.walletservice.service.pojo.WithdrawCreateServiceRequest
import java.util.UUID
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class WithdrawController(
    private val withdrawService: WithdrawService
) {

    companion object : KLogging()

    @GetMapping("/withdraw")
    fun getMyWithdraw(): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my Withdraw by user: ${userPrincipal.uuid}" }
        withdrawService.getPendingForOwner(userPrincipal.uuid)?.let {
            return ResponseEntity.ok(WithdrawResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/withdraw")
    fun createWithdraw(@RequestBody request: WithdrawCreateRequest): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create Withdraw:$request by user: ${userPrincipal.uuid}" }
        val serviceRequest = WithdrawCreateServiceRequest(
            userPrincipal.uuid, request.bankAccount, request.amount, userPrincipal.uuid, WalletType.USER)
        val withdraw = withdrawService.createWithdraw(serviceRequest)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }

    @GetMapping("/withdraw/project/{projectUuid}")
    fun getProjectWithdraw(@PathVariable projectUuid: UUID): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my Withdraw by user: ${userPrincipal.uuid}" }
        withdrawService.getPendingForProject(projectUuid, userPrincipal.uuid)?.let {
            return ResponseEntity.ok(WithdrawResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/withdraw/project/{projectUuid}")
    fun createProjectWithdraw(
        @PathVariable projectUuid: UUID,
        @RequestBody request: WithdrawCreateRequest
    ): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to create project withdraw:$request by user: ${userPrincipal.uuid}" }
        val serviceRequest = WithdrawCreateServiceRequest(
            projectUuid, request.bankAccount, request.amount, userPrincipal.uuid, WalletType.PROJECT)
        val withdraw = withdrawService.createWithdraw(serviceRequest)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }

    @DeleteMapping("/withdraw/{id}")
    fun deleteWithdraw(@PathVariable("id") withdrawId: Int): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to delete Withdraw: $withdrawId by user: ${userPrincipal.uuid}" }
        withdrawService.deleteWithdraw(withdrawId, userPrincipal.uuid)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/withdraw/{id}/transaction/approve")
    fun generateApproveTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate withdraw approval transaction by user: ${userPrincipal.uuid}" }
        val transactionDataAndInfo = withdrawService.generateApprovalTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }
}
