package com.ampnet.walletservice.config

import com.ampnet.walletservice.grpc.blockchain.BlockchainService
import com.ampnet.walletservice.grpc.mail.MailService
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("GrpcServiceMockConfig")
@Configuration
class GrpcServiceMockConfig {

    @Bean
    @Primary
    fun getBlockchainService(): BlockchainService {
        return Mockito.mock(BlockchainService::class.java)
    }

    @Bean
    @Primary
    fun getUserService(): UserService {
        return Mockito.mock(UserService::class.java)
    }

    @Bean
    @Primary
    fun getProjectService(): ProjectService {
        return Mockito.mock(ProjectService::class.java)
    }

    @Bean
    @Primary
    fun getMailService(): MailService {
        return Mockito.mock(MailService::class.java)
    }
}
