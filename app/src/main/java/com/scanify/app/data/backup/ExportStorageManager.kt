package com.scanify.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import com.scanify.app.domain.model.ExportState
import com.scanify.app.domain.model.ExportableItem
import com.scanify.app.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportStorageManager(
    private val context: Context,
    private val database: RoomDatabase,
    private val dbName: String,
    private val documentRepository: DocumentRepository
) {

    fun executeFullExport(targetUri: Uri? = null): Flow<ExportState> = flow {
        emit(ExportState.Processing(progress = 0.0f, currentFileName = "Preparing database..."))

        try {
            checkpointDatabase()

            val exportList = mutableListOf<ExportableItem>()
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) {
                exportList.add(ExportableItem(fileLabel = "backup_database.db", systemFile = dbFile))
            }

            val structuralDocuments = documentRepository.getAllDocuments().first()
            structuralDocuments.forEach { doc ->
                val file = File(doc.filePath)
                if (file.exists()) {
                    val extension = file.extension
                    var fileName = doc.name

                    if (extension.isNotEmpty() && !fileName.endsWith(".$extension", ignoreCase = true)) {
                        fileName += ".$extension"
                    }
                    val exportLabel = "Documents/$fileName"
                    exportList.add(ExportableItem(fileLabel = exportLabel, systemFile = file))
                }
            }

            if (exportList.isEmpty()) {
                emit(ExportState.Error(Exception("No data files found to export.")))
                return@flow
            }

            val totalBytes = exportList.sumOf { it.systemFile.length() }
            var totalBytesWritten = 0L

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val targetZipName = "Scanify_Backup_$timestamp.zip"

            val outputStream: OutputStream = if (targetUri != null) {
                context.contentResolver.openOutputStream(targetUri)
                    ?: throw IOException("Failed to bind output stream to selected destination.")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetZipName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Scanify/Backup")
                }
                val collectionUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val insertedUri = resolver.insert(collectionUri, contentValues)
                    ?: throw IOException("Failed to initialize MediaStore directory entry.")
                resolver.openOutputStream(insertedUri) ?: throw IOException("Failed to bind output stream.")
            } else {
                throw IOException("Direct saving is unsupported on this SDK version without user target selection.")
            }

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                zipOut.setLevel(java.util.zip.Deflater.DEFAULT_COMPRESSION)
                val dataBuffer = ByteArray(16384)

                for (item in exportList) {
                    val displayName = item.fileLabel.substringAfter("Documents/")

                    emit(
                        ExportState.Processing(
                            progress = if (totalBytes > 0) totalBytesWritten.toFloat() / totalBytes.toFloat() else 0f,
                            currentFileName = displayName
                        )
                    )

                    BufferedInputStream(FileInputStream(item.systemFile)).use { fileIn ->
                        zipOut.putNextEntry(ZipEntry(item.fileLabel))

                        var bytesRead: Int
                        while (fileIn.read(dataBuffer).also { bytesRead = it } != -1) {
                            zipOut.write(dataBuffer, 0, bytesRead)
                            totalBytesWritten += bytesRead

                            emit(
                                ExportState.Processing(
                                    progress = totalBytesWritten.toFloat() / totalBytes.toFloat(),
                                    currentFileName = displayName
                                )
                            )
                        }
                        zipOut.closeEntry()
                    }
                }
                zipOut.flush()
            }

            val finalDisplayPath = if (targetUri != null) "Selected Location" else "Documents/Scanify/Backup/$targetZipName"
            emit(ExportState.Success(finalDisplayPath))
        } catch (t: Throwable) {
            emit(ExportState.Error(t))
        }
    }.flowOn(Dispatchers.IO)

    private fun checkpointDatabase() {
        val checkpointQuery = SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")
        database.openHelper.writableDatabase.query(checkpointQuery).use { cursor ->
            cursor.moveToFirst()
        }
    }
}