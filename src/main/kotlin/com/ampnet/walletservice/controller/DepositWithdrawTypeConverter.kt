package com.ampnet.walletservice.controller

import com.ampnet.walletservice.enums.DepositWithdrawType
import mu.KLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class DepositWithdrawTypeConverter : Converter<String, DepositWithdrawType> {

    companion object : KLogging()

    override fun convert(source: String): DepositWithdrawType? {
        return try {
            DepositWithdrawType.valueOf(source.toUpperCase())
        } catch (ex: IllegalArgumentException) {
            logger.warn { "There is no withdraw or deposit type of: $source" }
            null
        }
    }
}
