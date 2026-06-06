package com.scanify.app.domain.repository

import java.io.File

interface FileManager {

    suspend fun saveDocumentFile(fileName: String, bytes: ByteArray): File?

    suspend fun saveDocumentFile(fileName: String, sourceFile: File): File?

    fun getReadableFileSize(file: File): String

}