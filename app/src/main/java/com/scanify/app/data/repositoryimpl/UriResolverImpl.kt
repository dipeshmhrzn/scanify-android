package com.scanify.app.data.repositoryimpl

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.scanify.app.domain.repository.UriResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class UriResolverImpl(private val context: Context) : UriResolver {

    override suspend fun resolveUri(uriString: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()

                if (uri.scheme == "file") {
                    val existing = uri.path?.let { File(it) }
                    if (existing != null && existing.exists()) {
                        return@withContext Pair(existing.name, existing.absolutePath)
                    }
                }

                var displayName = "imported_${System.currentTimeMillis()}"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) cursor.getString(index)?.let { displayName = it }
                    }
                }

                val safeDisplayName = displayName.replace(Regex("[/\\\\]"), "_").trim().ifEmpty {
                    "imported_${System.currentTimeMillis()}"
                }

                val stagingDir = File(context.cacheDir, "import_staging").apply { mkdirs() }
                val localFile = File(stagingDir, "${UUID.randomUUID()}_$safeDisplayName")

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(localFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext null

                if (localFile.exists()) Pair(safeDisplayName, localFile.absolutePath) else null
            } catch (e: Exception) {
                null
            }
        }
}