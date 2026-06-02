package com.scanify.app.data.repositoryimpl

import android.content.Context
import com.scanify.app.domain.repository.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileManagerImpl(context: Context) : FileManager {
    private val workspaceDir = File(context.filesDir, "scanned_documents").apply { mkdirs() }

    override suspend fun saveDocumentFile(fileName: String, bytes: ByteArray): File? = withContext(Dispatchers.IO) {
        try {
            val file = getUniqueFile(fileName)

            // Writing bytes to physical storage must happen on an IO thread
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            null
        }
    }

    override fun getReadableFileSize(file: File): String {
        val kb = file.length() / 1024.0
        return if (kb > 1024) String.format("%.1f MB", kb / 1024.0) else String.format("%.1f KB", kb)
    }

    private fun getUniqueFile(fileName: String): File {
        val nameWithoutExtension = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', "")
        val dotExtension = if (extension.isNotEmpty()) ".$extension" else ""

        var counter = 1
        var uniqueFile = File(workspaceDir, fileName)

        // Increment counter until a unique file handle is found
        while (uniqueFile.exists()) {
            uniqueFile = File(workspaceDir, "$nameWithoutExtension ($counter)$dotExtension")
            counter++
        }
        return uniqueFile
    }
}