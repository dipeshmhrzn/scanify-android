package com.scanify.app.domain.usecases.importdocumentusecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import androidx.core.graphics.scale

class ImportMultipleImagesUseCase @Inject constructor(
    private val uriResolver: UriResolver,
    private val fileManager: FileManager,
    private val documentRepository: DocumentRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(uriStrings: List<String>): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (uriStrings.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No source images provided."))
            }

            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val baseName = "SCANIFY_${LocalDateTime.now().format(formatter)}"
            val tempPdfFile = File(context.cacheDir, "$baseName.pdf")
            val pdfDocument = PdfDocument()

            val maxPageDim = 1200

            uriStrings.forEachIndexed { index: Int, uriString: String ->
                val resolvedData = uriResolver.resolveUri(uriString) ?: return@forEachIndexed
                val (_, fileBytes: ByteArray) = resolvedData

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, options)

                options.inSampleSize = calculateInSampleSize(options, maxPageDim, maxPageDim)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565

                var bitmap: Bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, options)
                    ?: return@forEachIndexed


                if (bitmap.width > maxPageDim || bitmap.height > maxPageDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val (targetW, targetH) = if (ratio > 1) {
                        maxPageDim to (maxPageDim / ratio).toInt()
                    } else {
                        (maxPageDim * ratio).toInt() to maxPageDim
                    }
                    val scaledBitmap = bitmap.scale(targetW, targetH)
                    if (scaledBitmap != bitmap) {
                        bitmap.recycle()
                        bitmap = scaledBitmap
                    }
                }

                val pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page: PdfDocument.Page = pdfDocument.startPage(pageInfo)

                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            try {
                tempPdfFile.outputStream().use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            } finally {
                pdfDocument.close()
            }

            val savedFile: File = fileManager.saveDocumentFile("$baseName.pdf", tempPdfFile.readBytes())
                ?: throw IOException("Failed to save generated multi-page asset.")

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

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height: Int = options.outHeight
        val width: Int = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}