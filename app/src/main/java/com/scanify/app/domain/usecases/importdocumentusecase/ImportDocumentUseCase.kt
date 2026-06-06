package com.scanify.app.domain.usecases.importdocumentusecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
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

class ImportDocumentUseCase @Inject constructor(
    private val uriResolver: UriResolver,
    private val fileManager: FileManager,
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(uriString: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val resolvedData = uriResolver.resolveUri(uriString)
                ?: return@withContext Result.failure(IOException("Failed to resolve file URI."))

            // 1. Updated destructuring: localFilePath is now a String path, not a ByteArray
            val (fileName: String, localFilePath: String) = resolvedData
            val extension = fileName.substringAfterLast('.', "").uppercase()

            val savedFile: File? = if (extension in setOf("JPG", "JPEG", "PNG", "WEBP")) {
                // Compress using the local file path and get bytes back
                val compressedBytes = compressImageFromFile(localFilePath)
                fileManager.saveDocumentFile(fileName, compressedBytes)
            } else {
                // ⚠️ Note: If fileManager only accepts ByteArrays right now, we read bytes here.
                // See optimization note below to make this 100% memory safe for huge PDFs!
                val fileBytes = File(localFilePath).readBytes()
                fileManager.saveDocumentFile(fileName, fileBytes)
            }

            if (savedFile == null) {
                return@withContext Result.failure(IOException("Failed to write file to internal storage."))
            }

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

    // 2. Updated to accept a file path string instead of loading raw bytes into memory
    private fun compressImageFromFile(filePath: String): ByteArray {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options) // Uses path directly

        val maxDim = 1600
        options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        // Decode the sampled down bitmap from the file system
        var bitmap = BitmapFactory.decodeFile(filePath, options) ?: return File(filePath).readBytes()

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