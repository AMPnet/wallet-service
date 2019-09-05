package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.repository.WalletRepository
import java.util.Optional
import java.util.UUID

internal object ServiceUtils {
    fun <T> wrapOptional(optional: Optional<T>): T? {
        return if (optional.isPresent) optional.get() else null
    }

    fun getWalletHash(owner: UUID, walletRepository: WalletRepository): String {
        val wallet = walletRepository.findByOwner(owner).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Wallet missing for owner: $owner")
        }
        return wallet.hash ?: throw InvalidRequestException(ErrorCode.WALLET_NOT_ACTIVATED, "Wallet not activated")
    }
}
