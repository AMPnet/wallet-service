package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.walletservice.controller.pojo.response.TransactionResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawWithUserListResponse
import com.ampnet.walletservice.controller.pojo.response.WithdrawWithUserResponse
import com.ampnet.walletservice.enums.WalletType
import com.ampnet.walletservice.grpc.userservice.UserService
import com.ampnet.walletservice.persistence.model.Withdraw
import com.ampnet.walletservice.service.WalletService
import com.ampnet.walletservice.service.WithdrawService
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import com.ampnet.walletservice.service.pojo.WithdrawCreateServiceRequest
import java.util.UUID
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class WithdrawController(
    private val withdrawService: WithdrawService,
    private val userService: UserService,
    private val walletService: WalletService
) {

    companion object : KLogging()

    @PostMapping("/withdraw")
    fun createWithdraw(@RequestBody request: WithdrawCreateRequest): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create Withdraw:$request by user: ${userPrincipal.uuid}" }
        val serviceRequest = WithdrawCreateServiceRequest(
            userPrincipal.uuid, request.bankAccount, request.amount, userPrincipal.uuid, WalletType.USER)
        val withdraw = withdrawService.createWithdraw(serviceRequest)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }

    @PostMapping("/withdraw/project/{projectUuid}")
    fun createProjectWithdraw(
        @PathVariable projectUuid: UUID,
        @RequestBody request: WithdrawCreateRequest
    ): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to create project withdraw:$request by user: ${userPrincipal.uuid}" }
        val serviceRequest = WithdrawCreateServiceRequest(
            projectUuid, request.bankAccount, request.amount, userPrincipal.uuid, WalletType.PROJECT)
        val withdraw = withdrawService.createWithdraw(serviceRequest)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }

    @GetMapping("/withdraw")
    fun getMyWithdraw(): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my Withdraw by user: ${userPrincipal.uuid}" }
        withdrawService.getPendingForUser(userPrincipal.uuid)?.let {
            return ResponseEntity.ok(WithdrawResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    // TODO: extract to CooperativeWithdrawController
    @DeleteMapping("/withdraw/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WITHDRAW)")
    fun deleteWithdraw(@PathVariable("id") withdrawId: Int): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to delete Withdraw: $withdrawId by user: ${userPrincipal.uuid}" }
        withdrawService.deleteWithdraw(withdrawId)
        return ResponseEntity.ok().build()
    }

    // TODO: different response for user and project
    @GetMapping("/withdraw/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getApprovedWithdraws(
        @RequestParam(defaultValue = "USER") type: WalletType,
        pageable: Pageable
    ): ResponseEntity<WithdrawWithUserListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all approved withdraws by user: ${userPrincipal.uuid}" }
        val response = generateResponseFromWithdraws(
            withdrawService.getAllApproved(type, pageable)
        )
        return ResponseEntity.ok(response)
    }

    // TODO: different response for user and project
    @GetMapping("/withdraw/burned")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getBurnedWithdraws(
        @RequestParam(defaultValue = "USER") type: WalletType,
        pageable: Pageable
    ): ResponseEntity<WithdrawWithUserListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all burned withdraws by user: ${userPrincipal.uuid}" }
        val response = generateResponseFromWithdraws(
            withdrawService.getAllBurned(type, pageable)
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/withdraw/{id}/transaction/approve")
    fun generateApproveTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate withdraw approval transaction by user: ${userPrincipal.uuid}" }
        val transactionDataAndInfo = withdrawService.generateApprovalTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    @PostMapping("/withdraw/{id}/transaction/burn")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WITHDRAW)")
    fun generateBurnTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate withdraw burn transaction by user: ${userPrincipal.uuid}" }
        val transactionDataAndInfo = withdrawService.generateBurnTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    @PostMapping("/withdraw/{id}/document")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_WITHDRAW)")
    fun addDocument(
        @PathVariable("id") id: Int,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Adding document for withdraw" }
        val documentRequest = DocumentSaveRequest(file, userPrincipal.uuid)
        val withdraw = withdrawService.addDocument(id, documentRequest)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }

    private fun generateResponseFromWithdraws(withdrawsPage: Page<Withdraw>): WithdrawWithUserListResponse {
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
}
