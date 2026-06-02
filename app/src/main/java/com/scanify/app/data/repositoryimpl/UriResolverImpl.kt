package com.scanify.app.data.repositoryimpl

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.scanify.app.domain.repository.UriResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UriResolverImpl(private val context: Context) : UriResolver {
    override suspend fun resolveUri(uriString: String): Pair<String, ByteArray>? = withContext(Dispatchers.IO) {
        try {
            val uri = uriString.toUri()
            var fileName = "imported_${System.currentTimeMillis()}"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) fileName = cursor.getString(index)
                }
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

            if (bytes != null) Pair(fileName, bytes) else null
        } catch (e: Exception) {
            null
        }
    }
}