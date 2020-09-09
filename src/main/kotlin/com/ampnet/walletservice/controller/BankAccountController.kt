package com.ampnet.walletservice.controller

import com.ampnet.walletservice.controller.pojo.request.BankAccountCreateRequest
import com.ampnet.walletservice.controller.pojo.response.BankAccountResponse
import com.ampnet.walletservice.controller.pojo.response.BankAccountsResponse
import com.ampnet.walletservice.service.BankAccountService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BankAccountController(private val bankAccountService: BankAccountService) {

    companion object : KLogging()

    @GetMapping("/bank-account")
    fun getBankAccounts(): ResponseEntity<BankAccountsResponse> {
        val bankAccounts = bankAccountService.getAllBankAccounts().map { BankAccountResponse(it) }
        return ResponseEntity.ok(BankAccountsResponse(bankAccounts))
    }

    @PostMapping("/bank-account")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun createBankAccount(@RequestBody request: BankAccountCreateRequest): ResponseEntity<BankAccountResponse> {
        val user = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create bank account: $request by user $user" }
        val bankAccount = bankAccountService.createBankAccount(user, request)
        return ResponseEntity.ok(BankAccountResponse(bankAccount))
    }

    @DeleteMapping("/bank-account/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.walletservice.enums.PrivilegeType).PWA_DEPOSIT)")
    fun deleteBankAccount(@PathVariable id: Int): ResponseEntity<Unit> {
        val user = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to delete bank account by id: $id by user: ${user.uuid}" }
        bankAccountService.deleteBankAccount(id)
        return ResponseEntity.ok().build()
    }
}
