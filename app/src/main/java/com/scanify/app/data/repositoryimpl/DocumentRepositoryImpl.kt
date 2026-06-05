package com.scanify.app.data.repositoryimpl

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.scanify.app.data.local.dao.DocumentDao
import com.scanify.app.data.mapper.toDomain
import com.scanify.app.data.mapper.toEntity
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.repository.DocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: DocumentDao
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> = dao.getAllDocuments().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun getDocumentById(id: Long): Document? = dao.getDocumentById(id)?.toDomain()

    override suspend fun importDocument(document: Document): Long =
        dao.insertDocuments(document.toEntity())

    override suspend fun deleteDocument(document: Document) =
        dao.deleteDocuments(document.toEntity())

    override suspend fun renameDocument(document: Document, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                dao.updateDocumentName(document.id, newName)
            }
        }

    override fun searchDocuments(query: String): Flow<List<Document>> {
        return dao.searchDocuments(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveDocumentToExternalStorage(document: Document): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(document.filePath)
                if (!sourceFile.exists()) {
                    return@withContext Result.failure(Exception("Source file not found."))
                }

                val extension = sourceFile.extension
                val baseName = if (document.name.endsWith(".$extension", ignoreCase = true)) {
                    document.name.dropLast(extension.length + 1)
                } else {
                    document.name
                }
                val finalName = "$baseName.$extension"

                val mimeType =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                        ?: "application/octet-stream"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_DOCUMENTS}/Scanify"
                        )
                        put(
                            MediaStore.MediaColumns.IS_PENDING,
                            1
                        ) // Tells system file is actively writing
                    }

                    val collection =
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val uri = resolver.insert(collection, contentValues)
                        ?: return@withContext Result.failure(Exception("Failed to create MediaStore record."))

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                } else {
                    val docsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val scanifyDir = File(docsDir, "Scanify")
                    if (!scanifyDir.exists()) scanifyDir.mkdirs()

                    var destFile = File(scanifyDir, finalName)
                    var counter = 1
                    while (destFile.exists()) {
                        destFile = File(scanifyDir, "$baseName ($counter).$extension")
                        counter++
                    }

                    sourceFile.copyTo(destFile, overwrite = false)

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf(mimeType)
                    ) { _, _ -> }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}