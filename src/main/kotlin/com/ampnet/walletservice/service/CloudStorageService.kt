package com.ampnet.walletservice.service

interface CloudStorageService {
    fun saveFile(name: String, content: ByteArray): String
    fun deleteFile(link: String)
}
