package com.ampnet.walletservice.service

import com.ampnet.walletservice.service.pojo.ProjectWithSellOffers

interface SellOfferService {
    fun getProjectsWithSalesOffers(coop: String): List<ProjectWithSellOffers>
}
