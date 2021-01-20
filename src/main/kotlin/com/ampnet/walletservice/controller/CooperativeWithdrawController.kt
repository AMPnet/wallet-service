package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.service.CooperativeWithdrawService
import com.ampnet.walletservice.service.pojo.request.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.response.WithdrawListServiceResponse
import com.ampnet.walletservice.service.pojo.response.WithdrawServiceResponse
import com.ampnet.walletservice.service.pojo.response.WithdrawWithDataServiceResponse
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
    fun getApprovedWithdraws(
        @RequestParam("type") type: DepositWithdrawType?,
        pageable: Pageable
    ): ResponseEntity<WithdrawListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all approved withdraws by user: ${userPrincipal.uuid}" }
        val withdrawList =
            cooperativeWithdrawService.getAllApproved(userPrincipal.coop, type, pageable)
        return ResponseEntity.ok(withdrawList)
    }

    @GetMapping("/cooperative/withdraw/burned")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getBurnedWithdraws(
        @RequestParam("type") type: DepositWithdrawType?,
        pageable: Pageable
    ): ResponseEntity<WithdrawListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all burned withdraws by user: ${userPrincipal.uuid}" }
        val withdrawList =
            cooperativeWithdrawService.getAllBurned(userPrincipal.coop, type, pageable)
        return ResponseEntity.ok(withdrawList)
    }

    @PostMapping("/cooperative/withdraw/{id}/transaction/burn")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WITHDRAW)")
    fun generateBurnTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate withdraw burn transaction by user: ${userPrincipal.uuid}" }
        val transactionDataAndInfo = cooperativeWithdrawService.generateBurnTransaction(id, userPrincipal)
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
        val documentRequest = DocumentSaveRequest(file, userPrincipal)
        val withdraw = cooperativeWithdrawService.addDocument(id, documentRequest)
        return ResponseEntity.ok(withdraw)
    }

    @GetMapping("/cooperative/withdraw/approved/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getWithdrawById(@PathVariable("id") id: Int): ResponseEntity<WithdrawWithDataServiceResponse> {
        logger.debug { "Received request to get withdraw by id: $id" }
        cooperativeWithdrawService.getById(id)?.let { withdrawWithData ->
            return ResponseEntity.ok(withdrawWithData)
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/cooperative/withdraw/pending")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getPendingWithdrawals(
        @RequestParam("type") type: DepositWithdrawType?,
        pageable: Pageable
    ): ResponseEntity<WithdrawListServiceResponse> {
        val coop = ControllerUtils.getUserPrincipalFromSecurityContext().coop
        logger.debug { "Received request to get pending withdrawals for type: $type in coop: $coop" }
        val withdrawList = cooperativeWithdrawService.getPending(coop, type, pageable)
        return ResponseEntity.ok(withdrawList)
    }
}
