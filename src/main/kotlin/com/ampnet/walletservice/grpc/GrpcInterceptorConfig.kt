package com.ampnet.walletservice.grpc

import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer
import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GrpcInterceptorConfig {

    @Bean
    fun globalInterceptorConfigurerAdapter(): GlobalClientInterceptorConfigurer {
        return GlobalClientInterceptorConfigurer {
            registry -> registry.addClientInterceptors(GrpcLogInterceptor())
        }
    }
    @Bean
    fun authenticationReader(): GrpcAuthenticationReader {
        return BasicGrpcAuthenticationReader()
    }
}
