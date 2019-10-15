package com.ampnet.walletservice.controller

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.core.jwt.exception.TokenException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.springframework.security.core.context.SecurityContextHolder

internal object ControllerUtils {
    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
            SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
                    ?: throw TokenException("SecurityContext authentication principal must be UserPrincipal")

    fun epochMilliToZonedDateTime(millis: Long): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}
