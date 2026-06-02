package com.scanify.app.presentation.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtils {

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result ?: "Unknown_File"
    }


    fun getUniqueFile(targetDirectory: File, originalFileName: String): File {
        var file = File(targetDirectory, originalFileName)

        if (!file.exists()) return file

        val nameWithoutExtension = file.nameWithoutExtension
        val extension = file.extension
        val dotExtension = if (extension.isNotEmpty()) ".$extension" else ""

        var counter = 1
        while (file.exists()) {
            val newName = "$nameWithoutExtension($counter)$dotExtension"
            file = File(targetDirectory, newName)
            counter++
        }

        return file
    }
}