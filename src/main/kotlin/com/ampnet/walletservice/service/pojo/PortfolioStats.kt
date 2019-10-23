package com.ampnet.walletservice.service.pojo

import java.time.ZonedDateTime

data class PortfolioStats(val investments: Long, val earnings: Long, val dateOfFirstInvestment: ZonedDateTime?)
