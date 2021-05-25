package com.ampnet.walletservice.service

import com.ampnet.walletservice.service.pojo.response.StatsResponse

interface StatsService {
    fun getStats(coop: String): StatsResponse
}
