package com.ampnet.walletservice.service.pojo

import com.ampnet.projectservice.proto.ProjectResponse
import com.ampnet.walletservice.grpc.blockchain.pojo.SellOfferData

data class ProjectWithSellOffers(
    val project: ProjectResponse,
    val sellOffers: List<SellOfferData>
)
