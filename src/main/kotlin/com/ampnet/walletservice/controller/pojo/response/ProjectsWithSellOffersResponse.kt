package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.grpc.blockchain.pojo.SellOfferData
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import com.ampnet.walletservice.service.pojo.response.ProjectWithSellOffers

data class ProjectsWithSellOffersResponse(
    val coop: String,
    val projects: List<ProjectWithSellOffersResponse>
)

data class ProjectWithSellOffersResponse(val project: ProjectServiceResponse, val sellOffers: List<SellOfferData>) {
    constructor(projectWithOffer: ProjectWithSellOffers) : this(
        projectWithOffer.project,
        projectWithOffer.sellOffers
    )
}
