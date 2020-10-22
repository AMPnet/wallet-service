package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.service.ProjectInvestmentService
import com.ampnet.walletservice.service.pojo.request.ProjectInvestmentRequest
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
class InvestmentController(private val projectInvestmentService: ProjectInvestmentService) {

    companion object : KLogging()

    @PostMapping("/invest/project/{projectUuid}")
    fun generateProjectInvestmentTransaction(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestBody @Valid request: AmountRequest
    ): ResponseEntity<TransactionResponse> {
        logger.debug { "Received request to generate invest transaction for project: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val serviceRequest = ProjectInvestmentRequest(projectUuid, userPrincipal.uuid, request.amount)
        val transaction = projectInvestmentService.generateInvestInProjectTransaction(serviceRequest)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }

    @PostMapping("/invest/project/{projectUuid}/cancel")
    fun generateCancelInvestmentsInProjectTransaction(
        @PathVariable("projectUuid") projectUuid: UUID
    ): ResponseEntity<TransactionResponse> {
        logger.debug { "Received request to generate cancel invests in project transaction for project: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val transaction = projectInvestmentService
            .generateCancelInvestmentsInProjectTransaction(projectUuid, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }
}
