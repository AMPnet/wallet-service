package com.ampnet.walletservice.service.pojo

import com.ampnet.projectservice.proto.ProjectResponse

data class ProjectWithInvestment(val project: ProjectResponse, val investment: Long)
