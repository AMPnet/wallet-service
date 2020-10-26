package com.ampnet.walletservice.service.pojo.request

import com.ampnet.core.jwt.UserPrincipal
import org.springframework.web.multipart.MultipartFile

data class DocumentSaveRequest(
    val data: ByteArray,
    val name: String,
    val size: Int,
    val type: String,
    val user: UserPrincipal
) {
    constructor(file: MultipartFile, user: UserPrincipal) : this(
        file.bytes,
        file.originalFilename ?: file.name,
        file.size.toInt(),
        file.contentType ?: file.originalFilename?.split(".")?.lastOrNull() ?: "Unknown",
        user
    )
}
