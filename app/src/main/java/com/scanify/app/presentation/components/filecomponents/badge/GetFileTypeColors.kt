package com.scanify.app.presentation.components.filecomponents.badge

    import androidx.compose.material3.MaterialTheme
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.graphics.Color

    @Composable
    fun getFileTypeColors(fileType: String): Pair<Color, Color> {
        return when (fileType.uppercase()) {
            "PDF" -> Color(0xFFE57373) to Color(0xFFFFEBEE)         // Soft Red
            "DOCX", "DOC" -> Color(0xFF64B5F6) to Color(0xFFE3F2FD) // Soft Blue
            "PPTX", "PPT" -> Color(0xFFBA68C8) to Color(0xFFF3E5F5) // Soft Purple (PowerPoint Theme)
            "XLSX", "XLS" -> Color(0xFF4DB6AC) to Color(0xFFE0F2F1) // Soft Teal/Green (Excel Theme)
            else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
        }
    }