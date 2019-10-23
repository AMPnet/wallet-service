package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.OrganizationWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.UserWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.UserWithWalletResponse
import com.ampnet.walletservice.service.CooperativeWalletService
import java.util.UUID
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CooperativeWalletController(
    private val cooperativeWalletService: CooperativeWalletService
) {

    companion object : KLogging()

    @PostMapping("/cooperative/wallet/{uuid}/transaction")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WALLET)")
    fun activateWalletTransaction(@PathVariable uuid: UUID): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to activate wallet: $uuid by user: ${userPrincipal.uuid}" }
        val transaction = cooperativeWalletService.generateWalletActivationTransaction(uuid, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }

    @GetMapping("/cooperative/wallet/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedUserWallets(): ResponseEntity<UserWithWalletListResponse> {
        logger.debug { "Received request to get list of users with unactivated wallet" }
        val users = cooperativeWalletService.getAllUserWithUnactivatedWallet()
        val usersResponse = users.map { UserWithWalletResponse(it) }
        return ResponseEntity.ok(UserWithWalletListResponse(usersResponse))
    }

    @GetMapping("/cooperative/wallet/organization")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedOrganizationWallets(): ResponseEntity<OrganizationWithWalletListResponse> {
        logger.debug { "Received request to get list of organizations with unactivated wallet" }
        val organizations = cooperativeWalletService.getOrganizationsWithUnactivatedWallet()
        return ResponseEntity.ok(OrganizationWithWalletListResponse(organizations))
    }

    @GetMapping("/cooperative/wallet/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedProjectWallets(): ResponseEntity<ProjectWithWalletListResponse> {
        logger.debug { "Received request to get list of projects with unactivated wallet" }
        val projects = cooperativeWalletService.getProjectsWithUnactivatedWallet()
        return ResponseEntity.ok(ProjectWithWalletListResponse(projects))
    }
}
