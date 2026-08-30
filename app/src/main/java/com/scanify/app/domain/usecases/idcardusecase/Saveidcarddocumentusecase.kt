package com.scanify.app.domain.usecases.idcardusecase

import android.content.Context
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import com.scanify.app.presentation.util.IdCardLayoutComposer
import com.scanify.app.presentation.util.JpegPdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SaveIdCardDocumentUseCase @Inject constructor(
    private val uriResolver: UriResolver,
    private val fileManager: FileManager,
    private val documentRepository: DocumentRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(frontUriString: String, backUriString: String?): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val frontResolved = uriResolver.resolveUri(frontUriString)
                    ?: return@withContext Result.failure(IOException("Could not read the front image."))

                val backFile: File? = if (backUriString != null) {
                    val backResolved = uriResolver.resolveUri(backUriString)
                        ?: return@withContext Result.failure(IOException("Could not read the back image."))
                    File(backResolved.second)
                } else {
                    null
                }

                val composedFile = IdCardLayoutComposer.composeReadyToPrintPage(
                    context = context,
                    frontFile = File(frontResolved.second),
                    backFile = backFile
                ) ?: return@withContext Result.failure(IOException("Failed to compose the ID card layout."))

                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                val baseName = "ID_CARD_SCANIFY_${LocalDateTime.now().format(formatter)}"
                val tempPdfFile = File(context.cacheDir, "$baseName.pdf")

                JpegPdfWriter.write(listOf(composedFile), tempPdfFile)
                composedFile.delete()

                val savedFile = fileManager.saveDocumentFile("$baseName.pdf", tempPdfFile)
                    ?: throw IOException("Failed to save the ID card document.")
                tempPdfFile.delete()

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
}