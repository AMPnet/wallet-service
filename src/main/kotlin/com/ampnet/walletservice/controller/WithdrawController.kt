package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.service.WithdrawService
import com.ampnet.walletservice.service.pojo.request.WithdrawCreateServiceRequest
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
class WithdrawController(
    private val withdrawService: WithdrawService
) {

    companion object : KLogging()

    @GetMapping("/withdraw")
    fun getMyWithdraw(): ResponseEntity<WithdrawServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my Withdraw by user: ${userPrincipal.uuid}" }
        withdrawService.getPendingForOwner(userPrincipal.uuid)?.let {
            return ResponseEntity.ok(it)
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/withdraw")
    fun createWithdraw(@RequestBody @Valid request: WithdrawCreateRequest): ResponseEntity<WithdrawServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create Withdraw:$request by user: ${userPrincipal.uuid}" }
        val serviceRequest = WithdrawCreateServiceRequest(
            userPrincipal.uuid, request.bankAccount, request.amount, userPrincipal, DepositWithdrawType.USER
        )
        val withdraw = withdrawService.createWithdraw(serviceRequest)
        return ResponseEntity.ok(withdraw)
    }

    @GetMapping("/withdraw/project/{projectUuid}")
    fun getProjectWithdraw(@PathVariable projectUuid: UUID): ResponseEntity<WithdrawServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my Withdraw by user: ${userPrincipal.uuid}" }
        withdrawService.getPendingForProject(projectUuid, userPrincipal.uuid)?.let {
            return ResponseEntity.ok(it)
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/withdraw/project/{projectUuid}")
    fun createProjectWithdraw(
        @PathVariable projectUuid: UUID,
        @RequestBody @Valid request: WithdrawCreateRequest
    ): ResponseEntity<WithdrawServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to create project withdraw:$request by user: ${userPrincipal.uuid}" }
        val serviceRequest = WithdrawCreateServiceRequest(
            projectUuid, request.bankAccount, request.amount, userPrincipal, DepositWithdrawType.PROJECT
        )
        val withdraw = withdrawService.createWithdraw(serviceRequest)
        return ResponseEntity.ok(withdraw)
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
        val transactionDataAndInfo = withdrawService.generateApprovalTransaction(id, userPrincipal)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }
}
