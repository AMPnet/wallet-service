package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.ProjectWithSellOffersResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectsWithSellOffersResponse
import com.ampnet.walletservice.service.SellOfferService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SellOfferController(private val sellOfferService: SellOfferService) {

    @GetMapping("/sell/offer")
    fun getSalesOffers(): ResponseEntity<ProjectsWithSellOffersResponse> {
        val projectsWithSellOffers = sellOfferService.getProjectsWithSalesOffers()
            .map { ProjectWithSellOffersResponse(it) }
        return ResponseEntity.ok(ProjectsWithSellOffersResponse(projectsWithSellOffers))
    }
}
