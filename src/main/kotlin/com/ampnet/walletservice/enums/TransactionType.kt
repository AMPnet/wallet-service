package com.ampnet.walletservice.enums

enum class TransactionType(val title: String, val description: String) {
    WALLET_ACTIVATE("Wallet Activation", "You are signing transaction to activate wallet type: %s"),
    CREATE_ORG("Create Organization", "You are signing transaction to create organization: %s"),
    CREATE_PROJECT("Create Project", "You are signing transaction to create project: %s"),
    INVEST("Invest", "You are signing transaction to investment to project: %s with amount %.2f"),
    MINT("Mint", "You are singing mint transaction for wallet: %s"),
    BURN_APPROVAL("Approval", "You are singing approval transaction to burn amount: %d"),
    BURN("Burn", "You are singing burn transaction for amount: %d"),
    CANCEL_INVEST("Cancel Investments", "You are signing transaction to cancel all investments in project: %s"),
    REVENUE_PAYOUT("Revenue Payout", "You are signing transaction to start revenue payout %.2f for project: %s"),
    TRNSF_TOKEN_OWN(
        "Transfer token ownership",
        "You are signing transaction to transfer token issuer ownership to user: %s"
    ),
    TRNSF_PLTFRM_OWN(
        "Transfer platform ownership",
        "You are signing transaction to transfer platform manager ownership to user: %s"
    )
}
