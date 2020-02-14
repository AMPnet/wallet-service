package com.ampnet.walletservice.grpc.blockchain.pojo

import com.ampnet.crowdfunding.proto.GetProjectsInfoResponse

data class ProjectInfoResponse(
    val txHash: String,
    val balance: Long,
    val investmentCap: Long,
    val minPerUser: Long,
    val maxPerUser: Long,
    val endsAt: Long,
    val payoutInProcess: Boolean
) {
    constructor(response: GetProjectsInfoResponse.ProjectInfo) : this(
        response.projectTxHash,
        response.totalFundsRaised.toLong(),
        response.investmentCap.toLong(),
        response.minPerUserInvestment.toLong(),
        response.maxPerUserInvestment.toLong(),
        response.endsAt.toLong(),
        response.payoutInProcess
    )
}
