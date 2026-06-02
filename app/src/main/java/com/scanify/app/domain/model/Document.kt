package com.scanify.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

@Immutable
data class Document(
    val id: Long = 0,
    val name: String,
    val fileType: String,
    val fileSize: String,
    val filePath: String,
    val createdAt: LocalDateTime,
    val isImageBundle: Boolean = false
)