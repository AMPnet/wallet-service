package com.ampnet.walletservice.service.impl

import com.ampnet.walletservice.persistence.model.File
import com.ampnet.walletservice.persistence.repository.DocumentRepository
import com.ampnet.walletservice.service.CloudStorageService
import com.ampnet.walletservice.service.StorageService
import com.ampnet.walletservice.service.pojo.DocumentSaveRequest
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class StorageServiceImpl(
    private val documentRepository: DocumentRepository,
    private val cloudStorageService: CloudStorageService
) : StorageService {

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MAX_DOCUMENT_TYPE_NAME = 16
    }

    override fun saveDocument(request: DocumentSaveRequest): File {
        logger.debug { "Storing document: ${request.name}" }

        val fileLink = storeOnCloud(request.name, request.data)
        logger.debug { "Successfully stored document on cloud: $fileLink" }

        val document = File::class.java.getDeclaredConstructor().newInstance()
        document.link = fileLink
        document.name = request.name
        document.size = request.size
        document.createdAt = ZonedDateTime.now()
        document.createdByUserUuid = request.user.uuid
        document.type = request.type.take(MAX_DOCUMENT_TYPE_NAME)
        return documentRepository.save(document)
    }

    private fun storeOnCloud(name: String, content: ByteArray): String = cloudStorageService.saveFile(name, content)
}
