package com.ampnet.walletservice.security

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.walletservice.enums.PrivilegeType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import java.util.UUID

class WithMockUserSecurityFactory : WithSecurityContextFactory<WithMockCrowdfoundUser> {

    private val password = "password"
    private val fullName = "First Last"

    override fun createSecurityContext(annotation: WithMockCrowdfoundUser): SecurityContext {
        val authorities = mapPrivilegesOrRoleToAuthorities(annotation)
        val userPrincipal = UserPrincipal(
            UUID.fromString(annotation.uuid),
            annotation.email,
            fullName,
            authorities.asSequence().map { it.authority }.toSet(),
            annotation.enabled,
            annotation.verified,
            annotation.coop
        )
        val token = UsernamePasswordAuthenticationToken(userPrincipal, password, authorities)

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = token
        return context
    }

    private fun mapPrivilegesOrRoleToAuthorities(annotation: WithMockCrowdfoundUser): List<SimpleGrantedAuthority> {
        return if (annotation.privileges.isNotEmpty()) {
            annotation.privileges.map { SimpleGrantedAuthority(it.name) }
        } else {
            getDefaultUserPrivileges().map { SimpleGrantedAuthority(it.name) }
        }
    }

    private fun getDefaultUserPrivileges() = emptyList<PrivilegeType>()
}
