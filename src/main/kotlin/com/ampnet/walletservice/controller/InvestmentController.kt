package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.service.ProjectInvestmentService
import com.ampnet.walletservice.service.pojo.ProjectInvestmentRequest
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class InvestmentController(private val projectInvestmentService: ProjectInvestmentService) {

    companion object : KLogging()

    @GetMapping("/invest/project/{projectUuid}")
    fun generateProjectInvestmentTransaction(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestParam(name = "amount") amount: Long
    ): ResponseEntity<TransactionResponse> {
        logger.debug { "Received request to generate invest transaction for project: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        val request = ProjectInvestmentRequest(projectUuid, userPrincipal.uuid, amount)
        val transaction = projectInvestmentService.generateInvestInProjectTransaction(request)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }
}
