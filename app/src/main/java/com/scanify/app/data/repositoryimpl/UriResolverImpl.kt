package com.scanify.app.data.repositoryimpl

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.scanify.app.domain.repository.UriResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UriResolverImpl(private val context: Context) : UriResolver {

    override suspend fun resolveUri(uriString: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val uri = uriString.toUri()
            var fileName = "imported_${System.currentTimeMillis()}"

            // 1. Get the original file name
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) fileName = cursor.getString(index)
                }
            }

            // 2. Create a local file in the app's internal storage
            val localFile = File(context.filesDir, fileName)

            // 3. Stream the data directly from the URI to the local file
            // This uses minimal RAM because it copies in small chunks!
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 4. Return the file name and the absolute path to your new local file
            if (localFile.exists()) Pair(fileName, localFile.absolutePath) else null

        } catch (e: Exception) {
            null
        }
    }
}