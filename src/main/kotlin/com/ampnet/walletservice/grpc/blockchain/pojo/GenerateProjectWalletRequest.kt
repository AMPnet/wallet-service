package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse

data class GenerateProjectWalletRequest(
    val userWalletHash: String,
    val organizationHash: String,
    val maxPerUser: Long,
    val minPerUser: Long,
    val investmentCap: Long,
    val endDateInMillis: Long
) {
    constructor(userWalletHash: String, organizationWalletHash: String, projectResponse: ProjectServiceResponse) : this(
        userWalletHash,
        organizationWalletHash,
        projectResponse.maxPerUser,
        projectResponse.minPerUser,
        projectResponse.expectedFunding,
        projectResponse.endDate.toInstant().toEpochMilli()
    )
}
