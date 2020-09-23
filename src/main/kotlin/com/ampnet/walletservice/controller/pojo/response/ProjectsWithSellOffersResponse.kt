package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.grpc.blockchain.pojo.SellOfferData
import com.ampnet.walletservice.service.pojo.ProjectWithSellOffers

data class ProjectsWithSellOffersResponse(
    val coop: String,
    val projects: List<ProjectWithSellOffersResponse>
)

data class ProjectWithSellOffersResponse(
    val project: ProjectControllerResponse,
    val sellOffers: List<SellOfferData>
) {
    constructor(projectWithOffer: ProjectWithSellOffers) : this(
        ProjectControllerResponse(projectWithOffer.project),
        projectWithOffer.sellOffers
    )
}
