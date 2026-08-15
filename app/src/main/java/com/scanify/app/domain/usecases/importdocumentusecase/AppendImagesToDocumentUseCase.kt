package com.scanify.app.domain.usecases.importdocumentusecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import com.scanify.app.presentation.util.ImageOptimizer
import com.scanify.app.presentation.util.JpegPdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class AppendImagesToDocumentUseCase @Inject constructor(
    private val uriResolver: UriResolver,
    private val fileManager: FileManager,
    private val documentRepository: DocumentRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(documentId: Long, additionalImageUris: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            var pfd: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null
            val tempJpegDir = File(context.cacheDir, "append_pages_$documentId").apply { mkdirs() }

            try {
                val existingDoc: Document = documentRepository.getDocumentById(documentId)
                    ?: throw IllegalArgumentException("Target storage item missing.")

                if (!existingDoc.isImageBundle) {
                    throw IllegalStateException("Static document formats cannot be structural bundles.")
                }

                val fileTarget = File(existingDoc.filePath)
                if (!fileTarget.exists()) throw IOException("Physical source file path does not exist.")

                val maxPageDim = ImageOptimizer.MAX_PAGE_DIMEN
                val pageJpegFiles = mutableListOf<File>()

                pfd = ParcelFileDescriptor.open(fileTarget, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(pfd)

                for (i in 0 until pdfRenderer.pageCount) {
                    pdfRenderer.openPage(i).use { page ->
                        val ratio = page.width.toFloat() / page.height.toFloat()
                        val (targetW, targetH) = if (ratio > 1) {
                            maxPageDim to (maxPageDim / ratio).toInt()
                        } else {
                            (maxPageDim * ratio).toInt() to maxPageDim
                        }

                        val bitmap = createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val jpegFile = File(tempJpegDir, "existing_${i}.jpg")
                        jpegFile.outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        bitmap.recycle()
                        pageJpegFiles.add(jpegFile)
                    }
                }
                pdfRenderer.close()
                pfd.close()
                pdfRenderer = null
                pfd = null

                additionalImageUris.forEach { uriString ->
                    val resolvedData = uriResolver.resolveUri(uriString) ?: return@forEach
                    val (_, localFilePath) = resolvedData
                    pageJpegFiles.add(File(localFilePath))
                }

                if (pageJpegFiles.isEmpty()) {
                    throw IOException("No pages available to write.")
                }

                val tempPdfFile = File(context.cacheDir, "append_transaction_$documentId.pdf")
                JpegPdfWriter.write(pageJpegFiles, tempPdfFile)

                tempPdfFile.copyTo(fileTarget, overwrite = true)
                tempPdfFile.delete()

                val updatedMetadataInstance = existingDoc.copy(
                    fileSize = fileManager.getReadableFileSize(fileTarget)
                )
                documentRepository.importDocument(updatedMetadataInstance)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                try { pdfRenderer?.close() } catch (ignored: Exception) {}
                try { pfd?.close() } catch (ignored: Exception) {}
                tempJpegDir.deleteRecursively()
            }
        }
}