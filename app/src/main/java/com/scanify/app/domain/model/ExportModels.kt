package com.scanify.app.domain.model

import java.io.File

sealed interface ExportState {
    object Idle : ExportState
    data class Processing(val progress: Float, val currentFileName: String) : ExportState
    data class Success(val destinationPath: String) : ExportState
    data class Error(val throwable: Throwable) : ExportState
}

data class ExportableItem(
    val fileLabel: String,
    val systemFile: File
)