package com.scanify.app.data.repositoryimpl

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import com.scanify.app.presentation.util.JpegPdfWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DocumentSaveManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val documentRepository: DocumentRepository
) {
    sealed interface SaveOutcome {
        data class Message(val text: String) : SaveOutcome
        data class Failure(val text: String) : SaveOutcome
    }

    suspend fun autoSaveToDocuments(document: Document): SaveOutcome = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(document.filePath)
            if (!sourceFile.exists()) return@withContext SaveOutcome.Failure("Source file not found.")

            val extension = document.fileType.lowercase()
            val cleanName = document.name.substringBeforeLast(".")
            val fileName = "$cleanName.$extension"

            val mimeType = when (extension) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "*/*"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Scanify/Documents")
                }
            }

            val resolver = appContext.contentResolver
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val targetUri = resolver.insert(collectionUri, contentValues)
                ?: return@withContext SaveOutcome.Failure("Failed to create target file.")

            resolver.openOutputStream(targetUri)?.use { out -> sourceFile.inputStream().use { it.copyTo(out) } }
            SaveOutcome.Message("Saved to Documents.")
        } catch (e: Exception) {
            SaveOutcome.Failure(e.localizedMessage ?: "Save failed.")
        }
    }

    suspend fun saveToUri(document: Document, targetUri: Uri): SaveOutcome = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(document.filePath)
            if (!sourceFile.exists()) return@withContext SaveOutcome.Failure("Source file not found.")

            appContext.contentResolver.openOutputStream(targetUri)?.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            }
            SaveOutcome.Message("Document saved successfully!")
        } catch (e: Exception) {
            SaveOutcome.Failure(e.localizedMessage ?: "Save failed.")
        }
    }

    suspend fun saveImagesToDevice(document: Document): SaveOutcome = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(document.filePath)
            if (!sourceFile.exists()) return@withContext SaveOutcome.Failure("Source file not found.")

            val jpegPages = JpegPdfWriter.extractEmbeddedJpegs(sourceFile)
            if (jpegPages.isEmpty()) return@withContext SaveOutcome.Failure("No pages found to save.")

            val cleanName = document.name.substringBeforeLast(".")
            val resolver = appContext.contentResolver
            val collectionUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            var savedCount = 0
            jpegPages.forEachIndexed { index, jpegBytes ->
                val fileName = if (jpegPages.size == 1) "$cleanName.jpg" else "${cleanName}_page${index + 1}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Scanify")
                }
                val targetUri = resolver.insert(collectionUri, contentValues)
                if (targetUri != null) {
                    resolver.openOutputStream(targetUri)?.use { out -> out.write(jpegBytes) }
                    savedCount++
                }
            }

            if (savedCount == jpegPages.size) {
                SaveOutcome.Message("Saved $savedCount image(s) to Pictures.")
            } else {
                SaveOutcome.Message("Saved $savedCount of ${jpegPages.size} image(s).")
            }
        } catch (e: Exception) {
            SaveOutcome.Failure(e.localizedMessage ?: "Save failed.")
        }
    }

    suspend fun saveImagesToDeviceLegacy(document: Document, treeUri: Uri): SaveOutcome = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(document.filePath)
            if (!sourceFile.exists()) return@withContext SaveOutcome.Failure("Source file not found.")

            val jpegPages = JpegPdfWriter.extractEmbeddedJpegs(sourceFile)
            if (jpegPages.isEmpty()) return@withContext SaveOutcome.Failure("No pages found to save.")

            val treeDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(appContext, treeUri)
                ?: return@withContext SaveOutcome.Failure("Could not open selected folder.")

            val cleanName = document.name.substringBeforeLast(".")
            var savedCount = 0
            jpegPages.forEachIndexed { index, jpegBytes ->
                val fileName = if (jpegPages.size == 1) "$cleanName.jpg" else "${cleanName}_page${index + 1}.jpg"
                val newFile = treeDoc.createFile("image/jpeg", fileName)
                if (newFile != null) {
                    appContext.contentResolver.openOutputStream(newFile.uri)?.use { out -> out.write(jpegBytes) }
                    savedCount++
                }
            }

            if (savedCount == jpegPages.size) {
                SaveOutcome.Message("Saved $savedCount image(s).")
            } else {
                SaveOutcome.Message("Saved $savedCount of ${jpegPages.size} image(s).")
            }
        } catch (e: Exception) {
            SaveOutcome.Failure(e.localizedMessage ?: "Save failed.")
        }
    }
}