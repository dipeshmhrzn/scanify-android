package com.scanify.app.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun getFileTypeColors(fileType: String): Pair<Color, Color> {
    return when (fileType.uppercase()) {
        "PDF" -> Color(0xFFE57373) to Color(0xFFFFEBEE) // Soft Red
        "DOCX", "DOC" -> Color(0xFF64B5F6) to Color(0xFFE3F2FD) // Soft Blue
        "JPG", "JPEG" -> Color(0xFFFFB74D) to Color(0xFFFFF3E0) // Soft Orange
        "PNG" -> Color(0xFF81C784) to Color(0xFFE8F5E9) // Soft Green
        else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    }
}