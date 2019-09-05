package com.ampnet.walletservice.controller

import com.ampnet.walletservice.exception.TokenException
import com.ampnet.walletservice.config.auth.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder

internal object ControllerUtils {
    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
            SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
                    ?: throw TokenException("SecurityContext authentication principal must be UserPrincipal")
}
