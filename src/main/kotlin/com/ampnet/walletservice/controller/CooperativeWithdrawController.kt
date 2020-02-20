package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawWithProjectListResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawWithProjectResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawWithUserListResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawWithUserResponse
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.CooperativeWithdrawService
import com.ampnet.walletservice.service.WalletService
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class CooperativeWithdrawController(
    private val cooperativeWithdrawService: CooperativeWithdrawService,
    private val userService: UserService,
    private val walletService: WalletService,
    private val projectService: ProjectService
) {

    companion object : KLogging()

    @GetMapping("/cooperative/withdraw/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getApprovedUserWithdraws(pageable: Pageable): ResponseEntity<WithdrawWithUserListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all approved withdraws by user: ${userPrincipal.uuid}" }
        val response = generateUserResponseWithWithdraws(
            cooperativeWithdrawService.getAllApproved(WalletType.USER, pageable)
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cooperative/withdraw/approved/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getApprovedProjectWithdraws(pageable: Pageable): ResponseEntity<WithdrawWithProjectListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all approved withdraws by user: ${userPrincipal.uuid}" }
        val response = generateProjectResponseWithWithdraws(
            cooperativeWithdrawService.getAllApproved(WalletType.PROJECT, pageable)
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cooperative/withdraw/burned")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getBurnedUserWithdraws(pageable: Pageable): ResponseEntity<WithdrawWithUserListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all burned withdraws by user: ${userPrincipal.uuid}" }
        val response = generateUserResponseWithWithdraws(
            cooperativeWithdrawService.getAllBurned(WalletType.USER, pageable)
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cooperative/withdraw/burned/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getBurnedProjectWithdraws(pageable: Pageable): ResponseEntity<WithdrawWithProjectListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all burned withdraws by user: ${userPrincipal.uuid}" }
        val response = generateProjectResponseWithWithdraws(
            cooperativeWithdrawService.getAllBurned(WalletType.PROJECT, pageable)
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/cooperative/withdraw/{id}/transaction/burn")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WITHDRAW)")
    fun generateBurnTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate withdraw burn transaction by user: ${userPrincipal.uuid}" }
        val transactionDataAndInfo = cooperativeWithdrawService.generateBurnTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    @PostMapping("/cooperative/withdraw/{id}/document")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WITHDRAW)")
    fun addDocument(
        @PathVariable("id") id: Int,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Adding document for withdraw" }
        val documentRequest = DocumentSaveRequest(file, userPrincipal.uuid)
        val withdraw = cooperativeWithdrawService.addDocument(id, documentRequest)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }

    private fun generateUserResponseWithWithdraws(withdrawsPage: Page<Withdraw>): WithdrawWithUserListResponse {
        val withdraws = withdrawsPage.toList()
        val users = userService
            .getUsers(withdraws.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val withdrawWithUserList = mutableListOf<WithdrawWithUserResponse>()
        withdraws.forEach { withdraw ->
            val wallet = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
            val userResponse = users[withdraw.ownerUuid.toString()]
            withdrawWithUserList.add(WithdrawWithUserResponse(withdraw, userResponse, wallet))
        }
        return WithdrawWithUserListResponse(withdrawWithUserList, withdrawsPage.number, withdrawsPage.totalPages)
    }

    private fun generateProjectResponseWithWithdraws(withdrawsPage: Page<Withdraw>): WithdrawWithProjectListResponse {
        val withdraws = withdrawsPage.toList()
        val projects = projectService
            .getProjects(withdraws.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val withdrawWithProjectList = mutableListOf<WithdrawWithProjectResponse>()
        withdraws.forEach { withdraw ->
            val wallet = walletService.getWallet(withdraw.ownerUuid)?.hash.orEmpty()
            val projectResponse = projects[withdraw.ownerUuid.toString()]
            withdrawWithProjectList.add(WithdrawWithProjectResponse(withdraw, projectResponse, wallet))
        }
        return WithdrawWithProjectListResponse(withdrawWithProjectList, withdrawsPage.number, withdrawsPage.totalPages)
    }
}
