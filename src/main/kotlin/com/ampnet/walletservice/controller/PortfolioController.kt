package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.BlockchainTransactionsResponse
import com.ampnet.walletservice.controller.pojo.response.PortfolioResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectWithInvestmentResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectWithInvestments
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.service.PortfolioService
import java.util.UUID
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PortfolioController(
    private val portfolioService: PortfolioService,
    private val projectService: ProjectService
) {

    companion object : KLogging()

    @GetMapping("/portfolio")
    fun getMyPortfolio(): ResponseEntity<PortfolioResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my portfolio by user: ${userPrincipal.uuid}" }
        val responseList = portfolioService.getPortfolio(userPrincipal.uuid)
            .map { ProjectWithInvestmentResponse(it) }
        return ResponseEntity.ok(PortfolioResponse(responseList))
    }

    @GetMapping("/portfolio/project/{uuid}")
    fun getPortfolioForProject(@PathVariable uuid: UUID): ResponseEntity<ProjectWithInvestments> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my portfolio for project: $uuid by user: ${userPrincipal.uuid}" }
        val project = projectService.getProject(uuid)
        val transactions = portfolioService.getInvestmentsInProject(userPrincipal.uuid, uuid)
        return ResponseEntity.ok(ProjectWithInvestments(project, transactions))
    }

    @GetMapping("/portfolio/transactions")
    fun getMyTransactions(): ResponseEntity<BlockchainTransactionsResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my transactions by user: ${userPrincipal.uuid}" }
        val blockchainTransactions = portfolioService.getTransactions(userPrincipal.uuid)
        return ResponseEntity.ok(BlockchainTransactionsResponse(blockchainTransactions))
    }
}
