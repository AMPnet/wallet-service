package com.ampnet.walletservice.enums

enum class TransactionType(val description: String) {
    WALLET_ACTIVATE("WalletActivateTx"),
    CREATE_ORG("CreateOrgTx"),
    CREATE_PROJECT("CreateProjectTx"),
    INVEST("InvestTx"),
    MINT("MintTx"),
    BURN_APPROVAL("BurnApprovalTx"),
    BURN("BurnTx")
}
