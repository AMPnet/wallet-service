package com.ampnet.walletservice.service

import com.ampnet.walletservice.persistence.model.File
import com.ampnet.walletservice.service.pojo.request.DocumentSaveRequest

interface StorageService {
    fun saveDocument(request: DocumentSaveRequest): File
}
