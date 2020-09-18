package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.ProjectWithSellOffersResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectsWithSellOffersResponse
import com.ampnet.walletservice.service.SellOfferService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SellOfferController(private val sellOfferService: SellOfferService) {

    companion object : KLogging()

    @GetMapping("/sell/offer")
    fun getSalesOffers(): ResponseEntity<ProjectsWithSellOffersResponse> {
        val user = ControllerUtils.getUserPrincipalFromSecurityContext()
        BankAccountController.logger.debug { "Received request to get sales offers for cooperative with id: ${user.coop}" }
        val projectsWithSellOffers = sellOfferService.getProjectsWithSalesOffers(user.coop)
            .map { ProjectWithSellOffersResponse(it) }
        return ResponseEntity.ok(ProjectsWithSellOffersResponse(projectsWithSellOffers))
    }
}
