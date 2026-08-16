package com.scanify.app.domain.usecases.importdocumentusecase

import android.content.Context
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import com.scanify.app.presentation.util.ImageOptimizer
import com.scanify.app.presentation.util.JpegPdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ImportMultipleImagesUseCase @Inject constructor(
    private val uriResolver: UriResolver,
    private val fileManager: FileManager,
    private val documentRepository: DocumentRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(uriStrings: List<String>): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                if (uriStrings.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("No source images provided."))
                }

                val resolvedFiles = uriStrings.mapNotNull { uriString ->
                    uriResolver.resolveUri(uriString)
                        ?.let { (_, localFilePath) -> File(localFilePath) }
                }
                if (resolvedFiles.isEmpty()) {
                    return@withContext Result.failure(IOException("None of the source images could be resolved."))
                }

                val decodeSemaphore = Semaphore(3)
                val embeddableFiles = coroutineScope {
                    resolvedFiles.map { file ->
                        async(Dispatchers.Default) {
                            decodeSemaphore.withPermit { ensureEmbeddableJpeg(file) }
                        }
                    }.awaitAll()
                }.filterNotNull()

                if (embeddableFiles.isEmpty()) {
                    return@withContext Result.failure(IOException("None of the source images could be processed."))
                }

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                val baseName = "SCANIFY_${LocalDateTime.now().format(formatter)}"
                val tempPdfFile = File(context.cacheDir, "$baseName.pdf")

                JpegPdfWriter.write(embeddableFiles, tempPdfFile)

                val savedFile: File = fileManager.saveDocumentFile("$baseName.pdf", tempPdfFile)
                    ?: throw IOException("Failed to save generated multi-page asset.")

                tempPdfFile.delete()
                embeddableFiles.forEach { if (it.parentFile == context.cacheDir) it.delete() }

                val document = Document(
                    name = savedFile.nameWithoutExtension,
                    fileType = "PDF",
                    fileSize = fileManager.getReadableFileSize(savedFile),
                    filePath = savedFile.absolutePath,
                    createdAt = LocalDateTime.now(),
                    isImageBundle = true
                )

                Result.success(documentRepository.importDocument(document))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun ensureEmbeddableJpeg(file: File): File? {
        val dims = JpegPdfWriter.tryReadJpegDimensions(file)
        val longEdge = dims?.let { maxOf(it[0], it[1]) }

        val alreadyEmbeddable = dims != null && longEdge != null &&
                longEdge <= (ImageOptimizer.MAX_PAGE_DIMEN * 1.05).toInt()

        if (alreadyEmbeddable) return file

        return try {
            val decoded = ImageOptimizer.decodeScaledBitmap(file) ?: return null
            val enhanced = ImageOptimizer.autoEnhance(decoded)
            val dir = file.parentFile ?: return null
            ImageOptimizer.writeJpeg(enhanced, dir, quality = 90, prefix = "norm_")
        } catch (e: Exception) {
            null
        }
    }
}