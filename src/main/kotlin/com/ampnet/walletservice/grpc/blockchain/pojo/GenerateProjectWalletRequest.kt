package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.projectservice.proto.ProjectResponse

data class GenerateProjectWalletRequest(
    val userWalletHash: String,
    val organizationHash: String,
    val maxPerUser: Long,
    val minPerUser: Long,
    val investmentCap: Long,
    val endDateInMillis: Long
) {
    constructor(userWalletHash: String, organizationWalletHash: String, projectResponse: ProjectResponse) : this(
        userWalletHash,
        organizationWalletHash,
        projectResponse.maxPerUser,
        projectResponse.minPerUser,
        projectResponse.expectedFunding,
        projectResponse.endDate
    )
}
