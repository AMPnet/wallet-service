package com.ampnet.walletservice.service

import com.ampnet.walletservice.service.pojo.response.ProjectWithSellOffers

interface SellOfferService {
    fun getProjectsWithSalesOffers(coop: String): List<ProjectWithSellOffers>
}
