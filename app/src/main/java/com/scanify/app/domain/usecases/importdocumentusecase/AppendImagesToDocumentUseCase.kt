package com.scanify.app.domain.usecases.importdocumentusecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.scale
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import androidx.core.graphics.createBitmap

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
            val pdfDocument = PdfDocument()

            try {
                val existingDoc: Document = documentRepository.getDocumentById(documentId)
                    ?: throw IllegalArgumentException("Target storage item missing.")

                if (!existingDoc.isImageBundle) {
                    throw IllegalStateException("Static document formats cannot be structural bundles.")
                }

                val fileTarget = File(existingDoc.filePath)
                if (!fileTarget.exists()) throw IOException("Physical source file path does not exist.")

                var pageIndexCounter = 0
                val maxPageDim = 1600 // Consistent Sweet Spot Resolution

                val pdfPaint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }

                pfd = ParcelFileDescriptor.open(fileTarget, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(pfd)

                // 1. RE-RENDER EXISTING PAGES AT HIGHER RESOLUTION
                for (i: Int in 0 until pdfRenderer.pageCount) {
                    pdfRenderer.openPage(i).use { page ->
                        // Calculate target dimensions to match our 1600 max dimension
                        val ratio = page.width.toFloat() / page.height.toFloat()
                        val (targetW, targetH) = if (ratio > 1) {
                            maxPageDim to (maxPageDim / ratio).toInt()
                        } else {
                            (maxPageDim * ratio).toInt() to maxPageDim
                        }

                        val bitmap = createBitmap(targetW, targetH)
                        bitmap.eraseColor(Color.WHITE) // Prevent transparent/black background issues

                        // By rendering into a larger bitmap, we natively increase the DPI
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(
                            targetW, targetH, ++pageIndexCounter
                        ).create()
                        val newPage: PdfDocument.Page = pdfDocument.startPage(pageInfo)

                        newPage.canvas.drawBitmap(bitmap, 0f, 0f, pdfPaint)
                        pdfDocument.finishPage(newPage)
                        bitmap.recycle()
                    }
                }
                pdfRenderer.close()
                pfd.close()

                // 2. APPEND NEW IMAGES USING DYNAMIC SCALING (No hardcoded inSampleSize = 2)
                additionalImageUris.forEach { uriString: String ->
                    val resolvedData = uriResolver.resolveUri(uriString) ?: return@forEach
                    val (_, fileBytes: ByteArray) = resolvedData

                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, options)

                    options.inSampleSize = calculateInSampleSize(options, maxPageDim, maxPageDim)
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565

                    var bitmap: Bitmap =
                        BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, options)
                            ?: return@forEach

                    if (bitmap.width > maxPageDim || bitmap.height > maxPageDim) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val (targetW, targetH) = if (ratio > 1) {
                            maxPageDim to (maxPageDim / ratio).toInt()
                        } else {
                            (maxPageDim * ratio).toInt() to maxPageDim
                        }
                        val scaledBitmap = bitmap.scale(targetW, targetH, filter = true)
                        if (scaledBitmap != bitmap) {
                            bitmap.recycle()
                            bitmap = scaledBitmap
                        }
                    }

                    val pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width, bitmap.height, ++pageIndexCounter
                    ).create()
                    val newPage: PdfDocument.Page = pdfDocument.startPage(pageInfo)

                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, pdfPaint)
                    pdfDocument.finishPage(newPage)
                    bitmap.recycle()
                }

                val tempCacheLocation = File(context.cacheDir, "append_transaction_workspace.pdf")
                tempCacheLocation.outputStream().use { output ->
                    pdfDocument.writeTo(output)
                }

                fileTarget.writeBytes(tempCacheLocation.readBytes())
                tempCacheLocation.delete()

                val updatedMetadataInstance: Document = existingDoc.copy(
                    fileSize = fileManager.getReadableFileSize(fileTarget)
                )
                documentRepository.importDocument(updatedMetadataInstance)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                pdfDocument.close()
                try {
                    pdfRenderer?.close()
                } catch (ignored: Exception) {}
                try {
                    pfd?.close()
                } catch (ignored: Exception) {}
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