package com.scanify.app.domain.usecases.importdocumentusecase


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.domain.repository.FileManager
import com.scanify.app.domain.repository.UriResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject
import androidx.core.graphics.scale

class ImportDocumentUseCase @Inject constructor(
    private val uriResolver: UriResolver,
    private val fileManager: FileManager,
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(uriString: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val resolvedData = uriResolver.resolveUri(uriString)
                ?: return@withContext Result.failure(IOException("Failed to resolve file URI."))

            var (fileName: String, fileBytes: ByteArray) = resolvedData
            val extension = fileName.substringAfterLast('.', "").uppercase()

            if (extension in setOf("JPG", "JPEG", "PNG", "WEBP")) {
                fileBytes = compressImageBytes(fileBytes)
            }

            val savedFile: File = fileManager.saveDocumentFile(fileName, fileBytes)
                ?: return@withContext Result.failure(IOException("Failed to write file to internal storage."))

            val cleanName: String = savedFile.nameWithoutExtension
            val finalExtension: String = savedFile.extension.uppercase()

            val document = Document(
                name = cleanName,
                fileType = finalExtension,
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

    private fun compressImageBytes(bytes: ByteArray): ByteArray {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        val maxDim = 1600
        options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return bytes

        if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (targetW, targetH) = if (ratio > 1) {
                maxDim to (maxDim / ratio).toInt()
            } else {
                (maxDim * ratio).toInt() to maxDim
            }
            val scaled = bitmap.scale(targetW, targetH)
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        bitmap.recycle()
        return outputStream.toByteArray()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}