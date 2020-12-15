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
    val coop: CoopProperties = CoopProperties()
}

class JwtProperties {
    lateinit var publicKey: String
}

class FileStorageProperties {
    lateinit var url: String
    lateinit var bucket: String
    lateinit var folder: String
}

@Suppress("MagicNumber")
class GrpcProperties {
    var blockchainServiceMaxRetries: Int = 3
    var blockchainServiceRetryDelay: Long = 1000
    var blockchainServiceTimeout: Long = 3000
    var mailServiceTimeout: Long = 1000
    var projectServiceTimeout: Long = 1000
    var userServiceTimeout: Long = 1000
}

class MailProperties {
    var sendNotification = true
}

class CoopProperties {
    lateinit var default: String
}
