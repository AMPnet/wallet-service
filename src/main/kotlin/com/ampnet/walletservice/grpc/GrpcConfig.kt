package com.ampnet.walletservice.grpc

import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader
import net.devh.boot.grpc.server.security.authentication.SSLContextGrpcAuthenticationReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GrpcConfig {

    @Bean
    fun authenticationReader(): GrpcAuthenticationReader {
        return SSLContextGrpcAuthenticationReader()
    }

    @Bean
    fun globalInterceptorConfigurerAdapter(): GlobalClientInterceptorConfigurer {
        return GlobalClientInterceptorConfigurer { registry ->
            registry.addClientInterceptors(GrpcLogInterceptor())
        }
    }
}
