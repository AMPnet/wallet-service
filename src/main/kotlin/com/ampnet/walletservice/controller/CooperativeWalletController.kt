package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WalletTransferRequest
import com.ampnet.walletservice.controller.pojo.response.OrganizationWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.UserWithWalletListResponse
import com.ampnet.walletservice.service.CooperativeWalletService
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
    fun getUnactivatedUserWallets(pageable: Pageable): ResponseEntity<UserWithWalletListResponse> {
        logger.debug { "Received request to get list of users with unactivated wallet" }
        val users = cooperativeWalletService.getAllUserWithUnactivatedWallet(pageable)
        return ResponseEntity.ok(UserWithWalletListResponse(users))
    }

    @GetMapping("/cooperative/wallet/organization")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedOrganizationWallets(pageable: Pageable): ResponseEntity<OrganizationWithWalletListResponse> {
        logger.debug { "Received request to get list of organizations with unactivated wallet" }
        val organizations = cooperativeWalletService.getOrganizationsWithUnactivatedWallet(pageable)
        return ResponseEntity.ok(OrganizationWithWalletListResponse(organizations))
    }

    @GetMapping("/cooperative/wallet/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedProjectWallets(pageable: Pageable): ResponseEntity<ProjectWithWalletListResponse> {
        logger.debug { "Received request to get list of projects with unactivated wallet" }
        val projects = cooperativeWalletService.getProjectsWithUnactivatedWallet(pageable)
        return ResponseEntity.ok(ProjectWithWalletListResponse(projects))
    }

    @PostMapping("/cooperative/wallet/transfer/transaction")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WALLET_TRANSFER)")
    fun generateTransferWalletTransaction(
        @RequestBody request: WalletTransferRequest
    ): ResponseEntity<TransactionResponse> {
        val user = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info {
            "Received request to transfer wallet ownership for ${request.type} to user: ${request.userUuid}" +
                " by: ${user.uuid}"
        }
        val transaction = cooperativeWalletService.generateSetTransferOwnership(user.uuid, request)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }
}
