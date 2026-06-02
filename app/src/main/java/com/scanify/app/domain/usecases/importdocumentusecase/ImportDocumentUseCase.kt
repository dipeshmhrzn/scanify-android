package com.scanify.app.domain.usecases.importdocumentusecase

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject

class ImportDocumentUseCase @Inject constructor(
    private val uriResolver: UriResolver,
    private val fileManager: FileManager,
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(uriString: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val resolvedData = uriResolver.resolveUri(uriString)
                ?: return@withContext Result.failure(IOException("Failed to resolve file URI."))
            val (fileName: String, fileBytes: ByteArray) = resolvedData

            val savedFile: File = fileManager.saveDocumentFile(fileName, fileBytes)
                ?: return@withContext Result.failure(IOException("Failed to write file to internal storage."))

            val extension: String = savedFile.extension.uppercase()
            val cleanName: String = savedFile.nameWithoutExtension

            val document = Document(
                name = cleanName,
                fileType = extension,
                fileSize = fileManager.getReadableFileSize(savedFile),
                filePath = savedFile.absolutePath,
                createdAt = LocalDateTime.now(),
                isImageBundle = false
            )

            Result.success(documentRepository.importDocument(document))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }}