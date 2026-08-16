package com.scanify.app.domain.usecases.importdocumentusecase

import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import com.scanify.app.presentation.util.ImageOptimizer
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

            val (fileName, localFilePath) = resolvedData
            val extension = fileName.substringAfterLast('.', "").uppercase()

            val savedFile: File? = if (extension in setOf("JPG", "JPEG", "PNG", "WEBP")) {
                val bitmap = ImageOptimizer.decodeScaledBitmap(File(localFilePath))
                    ?: return@withContext Result.failure(IOException("Failed to decode image."))
                val optimized = ImageOptimizer.writeJpeg(
                    bitmap,
                    File(localFilePath).parentFile ?: return@withContext Result.failure(
                        IOException(
                            "No cache dir."
                        )
                    )
                )
                fileManager.saveDocumentFile(fileName, optimized).also { optimized.delete() }
            } else {
                fileManager.saveDocumentFile(fileName, File(localFilePath))
            }

            if (savedFile == null) {
                return@withContext Result.failure(IOException("Failed to write file to internal storage."))
            }

            val document = Document(
                name = savedFile.nameWithoutExtension,
                fileType = savedFile.extension.uppercase(),
                fileSize = fileManager.getReadableFileSize(savedFile),
                filePath = savedFile.absolutePath,
                createdAt = LocalDateTime.now(),
                isImageBundle = false
            )

            Result.success(documentRepository.importDocument(document))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}