package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.service.CooperativeWithdrawService
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.WithdrawListServiceResponse
import com.ampnet.walletservice.service.pojo.WithdrawServiceResponse
import mu.KLogging
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
    private val cooperativeWithdrawService: CooperativeWithdrawService
) {

    companion object : KLogging()

    @GetMapping("/cooperative/withdraw/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getApprovedUserWithdraws(pageable: Pageable): ResponseEntity<WithdrawListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all approved withdraws by user: ${userPrincipal.uuid}" }
        val withdrawWithUserListResponse =
            cooperativeWithdrawService.getAllApproved(DepositWithdrawType.USER, pageable)
        return ResponseEntity.ok(withdrawWithUserListResponse)
    }

    @GetMapping("/cooperative/withdraw/approved/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getApprovedProjectWithdraws(pageable: Pageable): ResponseEntity<WithdrawListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all approved withdraws by user: ${userPrincipal.uuid}" }
        val withdrawWithProjectListResponse =
            cooperativeWithdrawService.getAllApproved(DepositWithdrawType.PROJECT, pageable)
        return ResponseEntity.ok(withdrawWithProjectListResponse)
    }

    @GetMapping("/cooperative/withdraw/burned")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getBurnedUserWithdraws(pageable: Pageable): ResponseEntity<WithdrawListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all burned withdraws by user: ${userPrincipal.uuid}" }
        val withdrawWithUserListResponse =
            cooperativeWithdrawService.getAllBurned(DepositWithdrawType.USER, pageable)
        return ResponseEntity.ok(withdrawWithUserListResponse)
    }

    @GetMapping("/cooperative/withdraw/burned/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getBurnedProjectWithdraws(pageable: Pageable): ResponseEntity<WithdrawListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all burned withdraws by user: ${userPrincipal.uuid}" }
        val withdrawWithProjectListResponse =
            cooperativeWithdrawService.getAllBurned(DepositWithdrawType.PROJECT, pageable)
        return ResponseEntity.ok(withdrawWithProjectListResponse)
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
    ): ResponseEntity<WithdrawServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Adding document for withdraw" }
        val documentRequest = DocumentSaveRequest(file, userPrincipal.uuid)
        val withdraw = cooperativeWithdrawService.addDocument(id, documentRequest)
        return ResponseEntity.ok(withdraw)
    }
}
