package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.AmountRequest
import com.ampnet.walletservice.controller.pojo.response.RevenuePayoutsResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.service.RevenueService
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
class RevenueController(private val revenueService: RevenueService) {

    companion object : KLogging()

    @PostMapping("/revenue/payout/project/{project}")
    fun revenuePayout(
        @PathVariable project: UUID,
        @RequestBody @Valid request: AmountRequest
    ): ResponseEntity<TransactionResponse> {
        val user = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate revenue payout transaction for project: $project by user: $user" }
        val transaction = revenueService.generateRevenuePayout(user, project, request.amount)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }

    @GetMapping("/revenue/payout/project/{project}")
    fun getRevenuePayouts(@PathVariable project: UUID, pageable: Pageable): ResponseEntity<RevenuePayoutsResponse> {
        logger.debug { "Received request to get revenue payouts for project: $project" }
        val revenuePayouts = revenueService.getRevenuePayouts(project, pageable)
        return ResponseEntity.ok(RevenuePayoutsResponse(revenuePayouts))
    }
}
