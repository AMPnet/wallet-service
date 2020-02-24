package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.DepositResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserListResponse
import com.ampnet.walletservice.controller.pojo.response.DepositWithUserResponse
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.UsersWithApprovedDeposit
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class CooperativeDepositController(
    private val cooperativeDepositService: CooperativeDepositService,
    private val userService: UserService
) {

    companion object : KLogging()

    @GetMapping("/cooperative/deposit/search")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getDepositByReference(
        @RequestParam("reference") reference: String
    ): ResponseEntity<DepositWithUserResponse> {
        logger.debug { "Received request to get find deposit by reference: $reference" }
        cooperativeDepositService.findByReference(reference)?.let {
            val user = userService.getUsers(setOf(it.userUuid)).firstOrNull()
            val response = DepositWithUserResponse(it, user)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.notFound().build()
    }

    @DeleteMapping("/cooperative/deposit/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun deleteDeposit(@PathVariable("id") id: Int): ResponseEntity<Unit> {
        logger.debug { "Received request to delete deposit: $id" }
        cooperativeDepositService.delete(id)
        return ResponseEntity.ok().build()
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

    @GetMapping("/cooperative/deposit/unapproved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getUnapprovedDeposits(pageable: Pageable): ResponseEntity<DepositWithUserListResponse> {
        logger.debug { "Received request to get unapproved deposits" }
        val deposits = cooperativeDepositService.getAllWithDocuments(false, pageable)
        val response = createDepositWithUserListResponse(deposits)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/cooperative/deposit/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getApprovedDeposits(pageable: Pageable): ResponseEntity<DepositWithUserListResponse> {
        logger.debug { "Received request to get approved deposits" }
        val deposits = cooperativeDepositService.getAllWithDocuments(true, pageable)
        val response = createDepositWithUserListResponse(deposits)
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
            .getUsers(deposits.map { it.userUuid }.toSet())
            .associateBy { it.uuid }
        val depositWithUserList = mutableListOf<DepositWithUserResponse>()
        deposits.forEach { deposit ->
            val user = users[deposit.userUuid.toString()]
            depositWithUserList.add(DepositWithUserResponse(deposit, user))
        }
        return DepositWithUserListResponse(depositWithUserList, depositsPage.number, depositsPage.totalPages)
    }
}
