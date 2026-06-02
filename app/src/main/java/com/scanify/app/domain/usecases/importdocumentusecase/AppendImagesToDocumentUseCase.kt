package com.scanify.app.domain.usecases.importdocumentusecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
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

                pfd = ParcelFileDescriptor.open(fileTarget, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(pfd)

                for (i: Int in 0 until pdfRenderer.pageCount) {
                    pdfRenderer.openPage(i).use { page ->
                        val bitmap: Bitmap = createBitmap(page.width, page.height)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(
                            page.width, page.height, ++pageIndexCounter
                        ).create()
                        val newPage: PdfDocument.Page = pdfDocument.startPage(pageInfo)

                        newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(newPage)
                        bitmap.recycle()
                    }
                }
                pdfRenderer.close()
                pfd.close()

                additionalImageUris.forEach { uriString: String ->
                    val resolvedData = uriResolver.resolveUri(uriString) ?: return@forEach
                    val (_, fileBytes: ByteArray) = resolvedData

                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bitmap: Bitmap =
                        BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, options)
                            ?: return@forEach

                    val pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(
                        bitmap.width, bitmap.height, ++pageIndexCounter
                    ).create()
                    val newPage: PdfDocument.Page = pdfDocument.startPage(pageInfo)

                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
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
                } catch (ignored: Exception) {
                }
                try {
                    pfd?.close()
                } catch (ignored: Exception) {
                }
            }
        }
}