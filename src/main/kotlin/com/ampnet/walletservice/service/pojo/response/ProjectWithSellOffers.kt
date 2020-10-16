package com.ampnet.walletservice.service.pojo.response

import com.ampnet.walletservice.grpc.blockchain.pojo.SellOfferData

data class ProjectWithSellOffers(
    val project: ProjectServiceResponse,
    val sellOffers: List<SellOfferData>
)
