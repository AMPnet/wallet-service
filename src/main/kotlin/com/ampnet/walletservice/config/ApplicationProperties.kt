package com.ampnet.walletservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.walletservice")
class ApplicationProperties {
    val jwt: JwtProperties = JwtProperties()
    val fileStorage: FileStorageProperties = FileStorageProperties()
    val grpc: GrpcProperties = GrpcProperties()
    val mail: MailProperties = MailProperties()
}

class JwtProperties {
    lateinit var signingKey: String
}

class FileStorageProperties {
    lateinit var url: String
    lateinit var bucket: String
    lateinit var folder: String
}

@Suppress("MagicNumber")
class GrpcProperties {
    var blockchainServiceTimeout: Long = 3000
    var blockchainPollingDelay: Long = 6000
    var mailServiceTimeout: Long = 1000
    var projectServiceTimeout: Long = 1000
    var userServiceTimeout: Long = 1000
}

class MailProperties {
    var sendNotification = true
}
