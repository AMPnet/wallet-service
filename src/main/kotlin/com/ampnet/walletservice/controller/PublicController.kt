package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectWithWalletResponse
import com.ampnet.walletservice.controller.pojo.response.WalletResponse
import com.ampnet.walletservice.service.WalletService
import java.util.UUID
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController(private val walletService: WalletService) {

    companion object : KLogging()

    @GetMapping("/public/wallet/project/{projectUuid}")
    fun getProjectWallet(@PathVariable projectUuid: UUID): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to get wallet for project: $projectUuid" }
        walletService.getWallet(projectUuid)?.let {
            val balance = walletService.getWalletBalance(it)
            return ResponseEntity.ok(WalletResponse(it, balance))
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/public/project/active")
    fun getAllActiveProjectsWithWallet(): ResponseEntity<ProjectWithWalletListResponse> {
        logger.debug { "Received request to get project all projects" }
        val projectsResponse = walletService.getProjectsWithActiveWallet()
            .map { ProjectWithWalletResponse(it) }
        return ResponseEntity.ok(ProjectWithWalletListResponse(projectsResponse))
    }
}
