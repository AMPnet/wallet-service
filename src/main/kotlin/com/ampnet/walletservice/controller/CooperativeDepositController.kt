package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.CommentRequest
import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithProjectListResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithProjectResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserListResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.UsersWithApprovedDeposit
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.grpc.projectservice.ProjectService
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Deposit
import com.ampnet.walletservice.service.CooperativeDepositService
import com.ampnet.walletservice.service.pojo.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.MintServiceRequest
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class CooperativeDepositController(
    private val cooperativeDepositService: CooperativeDepositService,
    private val userService: UserService,
    private val projectService: ProjectService
) {

    companion object : KLogging()

    @GetMapping("/cooperative/deposit/search")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getDepositByReference(
        @RequestParam("reference") reference: String
    ): ResponseEntity<DepositWithUserResponse> {
        logger.debug { "Received request to get find deposit by reference: $reference" }
        cooperativeDepositService.findByReference(reference)?.let {
            val user = userService.getUsers(setOf(it.ownerUuid)).firstOrNull()
            val response = DepositWithUserResponse(it, user)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/cooperative/deposit/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun approveDeposit(
        @PathVariable("id") id: Int,
        @RequestParam("amount") amount: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to approve deposit: $id" }

        val documentRequest = DocumentSaveRequest(file, userPrincipal.uuid)
        val serviceRequest = ApproveDepositRequest(id, userPrincipal.uuid, amount, documentRequest)
        val deposit = cooperativeDepositService.approve(serviceRequest)
        return ResponseEntity.ok(DepositResponse(deposit))
    }

    @PostMapping("/cooperative/deposit/{id}/decline")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun declineDeposit(
        @PathVariable("id") id: Int,
        @RequestBody request: CommentRequest
    ): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to delcine deposit: $id by user: ${userPrincipal.uuid}" }
        val deposit = cooperativeDepositService.decline(id, userPrincipal.uuid, request.comment)
        return ResponseEntity.ok(DepositResponse(deposit))
    }

    @GetMapping("/cooperative/deposit/unapproved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getUnapprovedDeposits(pageable: Pageable): ResponseEntity<DepositWithUserListResponse> {
        logger.debug { "Received request to get unapproved deposits" }
        val deposits = cooperativeDepositService
            .getAllWithDocuments(false, DepositWithdrawType.USER, pageable)
        val response = createDepositWithUserListResponse(deposits)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cooperative/deposit/unapproved/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getUnapprovedProjectDeposits(pageable: Pageable): ResponseEntity<DepositWithProjectListResponse> {
        logger.debug { "Received request to get unapproved deposits" }
        val deposits = cooperativeDepositService
            .getAllWithDocuments(false, DepositWithdrawType.PROJECT, pageable)
        val response = createDepositWithProjectListResponse(deposits)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cooperative/deposit/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getApprovedDeposits(pageable: Pageable): ResponseEntity<DepositWithUserListResponse> {
        logger.debug { "Received request to get approved deposits" }
        val deposits = cooperativeDepositService
            .getAllWithDocuments(true, DepositWithdrawType.USER, pageable)
        val response = createDepositWithUserListResponse(deposits)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cooperative/deposit/approved/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getApprovedProjectDeposits(pageable: Pageable): ResponseEntity<DepositWithProjectListResponse> {
        logger.debug { "Received request to get approved deposits" }
        val deposits = cooperativeDepositService
            .getAllWithDocuments(true, DepositWithdrawType.PROJECT, pageable)
        val response = createDepositWithProjectListResponse(deposits)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/cooperative/deposit/{id}/transaction")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun generateMintTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate mint transaction by user: ${userPrincipal.uuid}" }
        val serviceRequest = MintServiceRequest(id, userPrincipal.uuid)
        val transactionDataAndInfo = cooperativeDepositService.generateMintTransaction(serviceRequest)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    @GetMapping("/cooperative/deposit/count")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun countUsersWithApprovedDeposit(): ResponseEntity<UsersWithApprovedDeposit> {
        logger.debug { "Received request to count users with approved deposit" }
        val counted = cooperativeDepositService.countUsersWithApprovedDeposit()
        return ResponseEntity.ok(UsersWithApprovedDeposit(counted))
    }

    private fun createDepositWithUserListResponse(depositsPage: Page<Deposit>): DepositWithUserListResponse {
        val deposits = depositsPage.toList()
        val users = userService
            .getUsers(deposits.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val depositWithUserList = mutableListOf<DepositWithUserResponse>()
        deposits.forEach { deposit ->
            val user = users[deposit.ownerUuid.toString()]
            depositWithUserList.add(DepositWithUserResponse(deposit, user))
        }
        return DepositWithUserListResponse(depositWithUserList, depositsPage.number, depositsPage.totalPages)
    }

    private fun createDepositWithProjectListResponse(depositsPage: Page<Deposit>): DepositWithProjectListResponse {
        val deposits = depositsPage.toList()
        val projects = projectService
            .getProjects(deposits.map { it.ownerUuid }.toSet())
            .associateBy { it.uuid }
        val depositsWithProject = mutableListOf<DepositWithProjectResponse>()
        deposits.forEach { deposit ->
            val projectResponse = projects[deposit.ownerUuid.toString()]
            depositsWithProject.add(DepositWithProjectResponse(deposit, projectResponse))
        }
        return DepositWithProjectListResponse(depositsWithProject, depositsPage.number, depositsPage.totalPages)
    }
}
