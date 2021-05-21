package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.enums.DepositWithdrawType
import com.ampnet.walletservice.service.CooperativeDepositService
import com.ampnet.walletservice.service.pojo.request.ApproveDepositRequest
import com.ampnet.walletservice.service.pojo.request.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.request.MintServiceRequest
import com.ampnet.walletservice.service.pojo.response.DepositListServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositServiceResponse
import com.ampnet.walletservice.service.pojo.response.DepositWithDataServiceResponse
import mu.KLogging
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
class CooperativeDepositController(private val cooperativeDepositService: CooperativeDepositService) {

    companion object : KLogging()

    @GetMapping("/cooperative/deposit/search")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getDepositByReference(
        @RequestParam("reference") reference: String
    ): ResponseEntity<DepositWithDataServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to get find deposit by reference: $reference " +
                "from cooperative with id: ${userPrincipal.coop}"
        }
        cooperativeDepositService.findByReference(userPrincipal.coop, reference)?.let { depositWithData ->
            return ResponseEntity.ok(depositWithData)
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/cooperative/deposit/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun approveDeposit(
        @PathVariable("id") id: Int,
        @RequestParam("amount") amount: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DepositServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to approve deposit: $id" }
        val documentRequest = DocumentSaveRequest(file, userPrincipal)
        val serviceRequest = ApproveDepositRequest(id, userPrincipal, amount, documentRequest)
        val deposit = cooperativeDepositService.approve(serviceRequest)
        return ResponseEntity.ok(deposit)
    }

    @DeleteMapping("/cooperative/deposit/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun declineDeposit(@PathVariable("id") id: Int): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to decline deposit: $id by user: ${userPrincipal.uuid}" }
        cooperativeDepositService.delete(id, userPrincipal)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/cooperative/deposit/unapproved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getUnapprovedDeposits(
        @RequestParam("type") type: DepositWithdrawType?,
        pageable: Pageable
    ): ResponseEntity<DepositListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get unapproved deposits for coop: ${userPrincipal.coop}" }
        val depositWithUserListServiceResponse = cooperativeDepositService
            .getUnapproved(userPrincipal.coop, type, pageable)
        return ResponseEntity.ok(depositWithUserListServiceResponse)
    }

    @GetMapping("/cooperative/deposit/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getApprovedDeposits(
        @RequestParam("type") type: DepositWithdrawType?,
        pageable: Pageable
    ): ResponseEntity<DepositListServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get approved deposits for coop: ${userPrincipal.coop}" }
        val deposits = cooperativeDepositService
            .getApprovedWithDocuments(userPrincipal.coop, type, pageable)
        return ResponseEntity.ok(deposits)
    }

    @PostMapping("/cooperative/deposit/{id}/transaction")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun generateMintTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate mint transaction by user: ${userPrincipal.uuid}" }
        val serviceRequest = MintServiceRequest(id, userPrincipal)
        val transactionDataAndInfo = cooperativeDepositService.generateMintTransaction(serviceRequest)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    @GetMapping("/cooperative/deposit/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getDepositById(@PathVariable("id") id: Int): ResponseEntity<DepositWithDataServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get deposit by id: $id for coop: ${userPrincipal.coop}" }
        cooperativeDepositService.getByIdForCoop(userPrincipal.coop, id)?.let { depositWithData ->
            return ResponseEntity.ok(depositWithData)
        }
        return ResponseEntity.notFound().build()
    }
}
