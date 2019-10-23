package com.ampnet.walletservice.service.pojo

import java.util.UUID
import org.springframework.web.multipart.MultipartFile

data class DocumentSaveRequest(
    val data: ByteArray,
    val name: String,
    val size: Int,
    val type: String,
    val userUuid: UUID
) {
    constructor(file: MultipartFile, userUuid: UUID) : this(
            file.bytes,
            file.originalFilename ?: file.name,
            file.size.toInt(),
            file.contentType ?: file.originalFilename?.split(".")?.lastOrNull() ?: "Unknown",
            userUuid
    )
}
