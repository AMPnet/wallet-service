package com.ampnet.walletservice.controller

import com.ampnet.walletservice.service.StatsService
import com.ampnet.walletservice.service.pojo.response.StatsResponse
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AdminStatsController(private val statsService: StatsService) {

    companion object : KLogging()

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getStats(): ResponseEntity<StatsResponse> {
        logger.debug { "Received request to get admin stats" }
        val user = ControllerUtils.getUserPrincipalFromSecurityContext()
        val stats = statsService.getStats(user.coop)
        logger.debug { "Stats: $stats" }
        return ResponseEntity.ok(stats)
    }
}
