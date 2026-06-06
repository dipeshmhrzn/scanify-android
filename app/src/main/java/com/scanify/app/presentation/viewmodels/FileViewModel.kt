package com.scanify.app.presentation.viewmodels

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.usecases.DocumentUseCases
import com.scanify.app.presentation.util.enforceMinimumDelay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface FileUiState {
    object Loading : FileUiState
    object Empty : FileUiState
    data class Success(val documents: List<Document>) : FileUiState
}

sealed interface FileNavigationEvent {
    data class NavigateToPreview(val documentId: Long) : FileNavigationEvent
    data class OpenExternalFile(val filePath: String, val fileType: String) : FileNavigationEvent
    data class ShowError(val message: String) : FileNavigationEvent

    data class ShowMessage(val message: String) : FileNavigationEvent
    data class ShareFile(val filePath: String, val fileType: String, val displayName: String) :
        FileNavigationEvent
}

@HiltViewModel
class FileViewModel @Inject constructor(
    private val documentUseCases: DocumentUseCases,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val uiState: StateFlow<FileUiState> = documentUseCases.getDocuments()
        .map { if (it.isEmpty()) FileUiState.Empty else FileUiState.Success(it) }
        .combine(
            flow {
                delay(300)
                emit(Unit)
            }
        ) { dataState, _ ->
            dataState
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FileUiState.Loading)

    private val _navigationEvent = Channel<FileNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun onDocumentClick(document: Document) {
        viewModelScope.launch {
            routeDocument(document)
        }
    }

    fun importMultipleFiles(uriStrings: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            _isImporting.value = true
            documentUseCases.importMultipleFiles(uriStrings)
                .onFailure { err ->
                    _navigationEvent.send(
                        FileNavigationEvent.ShowError(
                            err.localizedMessage ?: "Failed batch import."
                        )
                    )
                }
            enforceMinimumDelay(startTime)
            _isImporting.value = false
        }
    }

    fun importMultipleImages(uriStrings: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            _isImporting.value = true
            documentUseCases.importMultipleImages(uriStrings)
                .onSuccess { generatedId ->
                    _navigationEvent.send(FileNavigationEvent.NavigateToPreview(generatedId))
                }
                .onFailure { err ->
                    _navigationEvent.send(
                        FileNavigationEvent.ShowError(
                            err.localizedMessage ?: "Failed compiling image bundle."
                        )
                    )
                }
            enforceMinimumDelay(startTime)
            _isImporting.value = false
        }
    }

    fun renameDocument(document: Document, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty() || trimmedName.equals(document.name, ignoreCase = true)) return

        var finalizedName = trimmedName

        val currentState = uiState.value
        if (currentState is FileUiState.Success) {
            val existingNames = currentState.documents
                .filter { it.id != document.id }
                .map { it.name.lowercase() }
                .toSet()

            if (existingNames.contains(finalizedName.lowercase())) {
                var counter = 1
                val baseName = finalizedName.substringBeforeLast(".")
                val extension = finalizedName.substringAfterLast(".", "")
                val dotSuffix = if (extension.isNotEmpty()) ".$extension" else ""

                do {
                    finalizedName = "$baseName ($counter)$dotSuffix"
                    counter++
                } while (existingNames.contains(finalizedName.lowercase()))
            }
        }

        viewModelScope.launch {
            documentUseCases.renameDocumentUseCase(document, finalizedName)
        }
    }

    fun shareDocument(document: Document) {
        viewModelScope.launch {
            _navigationEvent.send(
                FileNavigationEvent.ShareFile(
                    filePath = document.filePath,
                    fileType = document.fileType,
                    displayName = document.name
                )
            )
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            documentUseCases.deleteDocument(document)
                .onFailure { err ->
                    _navigationEvent.send(
                        FileNavigationEvent.ShowError(
                            err.localizedMessage ?: "Failed delete"
                        )
                    )
                }
        }
    }

    fun autoSaveToDocuments(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            _isSaving.value = true
            try {
                val sourceFile = File(document.filePath)
                if (!sourceFile.exists()) {
                    _navigationEvent.send(FileNavigationEvent.ShowError("Source file not found."))
                    return@launch
                }

                val extension = document.fileType.lowercase()
                val cleanName = document.name.substringBeforeLast(".")
                val fileName = "$cleanName.$extension"

                val mimeType = when (extension) {
                    "pdf" -> "application/pdf"
                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    else -> "*/*"
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Scanify/Documents")
                    }
                }

                val resolver = appContext.contentResolver
                val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Files.getContentUri("external")
                }

                val targetUri = resolver.insert(collectionUri, contentValues)

                if (targetUri != null) {
                    resolver.openOutputStream(targetUri)?.use { outStream ->
                        sourceFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    _navigationEvent.send(FileNavigationEvent.ShowMessage("Saved to Documents."))
                } else {
                    _navigationEvent.send(FileNavigationEvent.ShowError("Failed to create target file."))
                }

            } catch (e: Exception) {
                _navigationEvent.send(FileNavigationEvent.ShowError("Save failed: ${e.localizedMessage}"))
            } finally {
                enforceMinimumDelay(startTime)
                _isSaving.value = false
            }
        }
    }

    fun saveToSelectedUri(document: Document, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            _isSaving.value = true
            try {
                val sourceFile = File(document.filePath)
                if (!sourceFile.exists()) {
                    _navigationEvent.send(FileNavigationEvent.ShowError("Source file not found."))
                    return@launch
                }

                appContext.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                _navigationEvent.send(FileNavigationEvent.ShowMessage("Document saved successfully!"))
            } catch (e: Exception) {
                _navigationEvent.send(FileNavigationEvent.ShowError("Save failed: ${e.localizedMessage}"))
            } finally {
                enforceMinimumDelay(startTime)
                _isSaving.value = false
            }
        }
    }

    fun handleScannedDocuments(imageUris: List<String>, pdfUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            _isImporting.value = true

            try {
                if (imageUris.isNotEmpty()) {
                    documentUseCases.importMultipleImages(imageUris)
                        .onSuccess { generatedId ->
                            _navigationEvent.send(FileNavigationEvent.NavigateToPreview(generatedId))
                        }
                        .onFailure { err ->
                            _navigationEvent.send(
                                FileNavigationEvent.ShowError(
                                    err.localizedMessage ?: "Failed compiling scanned images."
                                )
                            )
                        }
                } else if (pdfUri != null) {
                    documentUseCases.importMultipleFiles(listOf(pdfUri))
                        .onFailure { err ->
                            _navigationEvent.send(
                                FileNavigationEvent.ShowError(
                                    err.localizedMessage ?: "Failed importing scanned PDF."
                                )
                            )
                        }
                }
            } catch (e: Exception) {
                _navigationEvent.send(
                    FileNavigationEvent.ShowError(e.localizedMessage ?: "Unknown scan error")
                )
            } finally {
                enforceMinimumDelay(startTime)
                _isImporting.value = false
            }
        }
    }

    private suspend fun routeDocument(document: Document) {
        when (document.fileType.uppercase()) {
            "PDF", "JPG", "JPEG", "PNG" -> {
                _navigationEvent.send(FileNavigationEvent.NavigateToPreview(document.id))
            }

            else -> {
                _navigationEvent.send(
                    FileNavigationEvent.OpenExternalFile(
                        filePath = document.filePath,
                        fileType = document.fileType
                    )
                )
            }
        }
    }
}