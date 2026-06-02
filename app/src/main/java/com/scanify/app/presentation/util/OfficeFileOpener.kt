package com.scanify.app.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object OfficeFileOpener {
    fun openFile(context: Context, filePath: String, fileType: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = when (fileType.uppercase()) {
            "DOC", "DOCX" -> "application/msword"
            "XLS", "XLSX" -> "application/vnd.ms-excel"
            "PPT", "PPTX" -> "application/vnd.ms-powerpoint"
            "TXT"         -> "text/plain"
            else          -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No viewer application found for $fileType files.", Toast.LENGTH_LONG).show()
        }
    }
}