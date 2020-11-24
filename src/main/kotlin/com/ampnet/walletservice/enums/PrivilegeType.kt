package com.ampnet.walletservice.enums

enum class PrivilegeType {

    /*
    Def: <type>_privilege

    type:
        - PR - PERM_READ
        - PW - PERM_WRITE
        - PRO - PERM_READ_OWN
        - PWO - PER_WRITE_OWN
        - PRA - PERM_READ_ADMIN
        - PWA - PERM_WRITE_ADMIN
     */

    // Administration
    MONITORING,

    // Coop
    PWA_COOP,
    PRA_COOP,

    // Wallet
    PRA_WALLET,
    PWA_WALLET,
    PWA_WALLET_TRANSFER,

    // Withdraw
    PRA_WITHDRAW,
    PWA_WITHDRAW,

    // Deposit
    PRA_DEPOSIT,
    PWA_DEPOSIT
}
