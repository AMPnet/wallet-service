package com.ampnet.walletservice.controller.pojo.response

import com.ampnet.walletservice.persistence.model.File
import java.time.ZonedDateTime

data class DocumentResponse(
    val id: Int,
    val link: String,
    val name: String,
    val type: String,
    val size: Int,
    val createdAt: ZonedDateTime
) {
    constructor(document: File) : this(
        document.id,
        document.link,
        document.name,
        document.type,
        document.size,
        document.createdAt
    )
}
