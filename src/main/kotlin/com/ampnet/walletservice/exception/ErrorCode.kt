package com.ampnet.walletservice.exception

enum class ErrorCode(val categoryCode: String, var specificCode: String, var message: String) {

    // User: 03
    USER_BANK_INVALID("03", "02", "Invalid bank account data"),
    USER_MISSING_PRIVILEGE("03", "05", "Missing privilege to access data"),

    // Wallet: 05
    WALLET_MISSING("05", "01", "Missing wallet"),
    WALLET_EXISTS("05", "02", "Active user cannot create additional wallet"),
    WALLET_FUNDS("05", "03", "User does not have enough funds on wallet"),
    WALLET_HASH_EXISTS("05", "04", "Wallet with this hash already exists"),
    WALLET_DEPOSIT_MISSING("05", "05", "Missing deposit"),
    WALLET_DEPOSIT_MINTED("05", "06", "Deposit is already minted"),
    WALLET_DEPOSIT_NOT_APPROVED("05", "07", "Deposit is not approved"),
    WALLET_DEPOSIT_EXISTS("05", "08", "Unapproved deposit exists"),
    WALLET_WITHDRAW_MISSING("05", "09", "Missing withdraw"),
    WALLET_WITHDRAW_EXISTS("05", "10", "Unapproved withdraw exists"),
    WALLET_WITHDRAW_APPROVED("05", "11", "Withdraw already approved"),
    WALLET_WITHDRAW_NOT_APPROVED("05", "12", "Withdraw not approved"),
    WALLET_WITHDRAW_BURNED("05", "13", "Withdraw already burned"),
    WALLET_NOT_ACTIVATED("05", "14", "Wallet is not activated by the administrator"),
    WALLET_PAYOUT_MISSING("05", "15", "Missing revenue payout"),

    // Organization: 06
    ORG_MISSING("06", "01", "Non existing organization"),
    ORG_MISSING_PRIVILEGE("06", "07", "Missing a privilege for this organization"),
    ORG_MEM_MISSING("06", "08", "Organization membership missing"),

    // Project: 07
    PRJ_MISSING("07", "01", "Non existing project"),
    PRJ_DATE_EXPIRED("07", "03", "Project has expired"),
    PRJ_MAX_PER_USER("07", "04", "User has exceeded max funds per project"),
    PRJ_MAX_FUNDS("07", "06", "Project has reached expected funding"),
    PRJ_NOT_ACTIVE("07", "07", "Project is not active"),
    PRJ_MISSING_PRIVILEGE("07", "12", "Missing project owner privilege"),

    // Internal: 08
    INT_FILE_STORAGE("08", "01", "Could not upload document on cloud file storage"),
    INT_INVALID_VALUE("08", "02", "Invalid value in request"),
    INT_GRPC_BLOCKCHAIN("08", "03", "Failed gRPC call to blockchain service"),
    INT_GRPC_USER("08", "04", "Failed gRPC call to user service"),
    INT_GRPC_PROJECT("08", "05", "Failed gRPC call to project service"),
    INT_GRPC_MAIL("08", "06", "Failed gRPC call to mail service"),
    INT_DB("08", "07", "Database exception"),
    INT_REQUEST("08", "08", "Invalid request data"),

    // Transaction: 09
    TX_MISSING("09", "01", "Non existing transaction"),
    TX_DATA_MISSING("09", "02", "Missing transaction data"),

    // Middleware: 11
    MIDDLEWARE("11", "00", "Undefined")
}
