package com.ampnet.walletservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WalletServiceApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<WalletServiceApplication>(*args)
}
