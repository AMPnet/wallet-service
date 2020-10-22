package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.exception.ErrorCode
import com.ampnet.walletservice.exception.InvalidRequestException
import com.ampnet.walletservice.exception.ResourceNotFoundException
import com.ampnet.walletservice.persistence.model.Wallet
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.persistence.repository.WalletRepository
import com.ampnet.walletservice.persistence.repository.WithdrawRepository
import com.ampnet.walletservice.service.pojo.response.ProjectServiceResponse
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.Optional
import java.util.UUID

internal object ServiceUtils {
    fun <T> wrapOptional(optional: Optional<T>): T? {
        return if (optional.isPresent) optional.get() else null
    }

    @Throws(InvalidRequestException::class, ResourceNotFoundException::class)
    fun getWalletHash(owner: UUID, walletRepository: WalletRepository): String {
        val wallet = walletRepository.findByOwner(owner).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Wallet missing for owner: $owner")
        }
        return wallet.hash ?: throw InvalidRequestException(ErrorCode.WALLET_NOT_ACTIVATED, "Wallet not activated")
    }

    @Throws(ResourceNotFoundException::class)
    fun getWithdraw(withdrawId: Int, withdrawRepository: WithdrawRepository): Withdraw {
        return withdrawRepository.findById(withdrawId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_WITHDRAW_MISSING, "Missing withdraw with id: $withdrawId")
        }
    }

    @Throws(InvalidRequestException::class)
    fun validateUserIsProjectOwner(user: UUID, project: ProjectServiceResponse) {
        if (project.createByUuid != user) {
            throw InvalidRequestException(
                ErrorCode.PRJ_MISSING_PRIVILEGE,
                "User: $user did not create this project: ${project.uuid} " +
                    "and cannot create a Withdraw for project"
            )
        }
    }

    @Throws(ResourceNotFoundException::class)
    fun getWalletByUserUuid(userUuid: UUID, walletRepository: WalletRepository):
        Wallet = walletRepository.findByOwner(userUuid).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Missing wallet for user with uuid: $userUuid")
        }
}

const val FROM_CENTS_TO_EUROS = 100L
fun Long.toEurAmount(): String = DecimalFormat("#,##0.00").format(this / FROM_CENTS_TO_EUROS)
fun Long.toBigDecimal(): BigDecimal = BigDecimal.valueOf(this)
