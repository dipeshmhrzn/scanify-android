package com.scanify.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.usecases.DocumentUseCases
import com.scanify.app.presentation.util.enforceMinimumDelay
import dagger.hilt.android.lifecycle.HiltViewModel
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
    data class ShareFile(val filePath: String, val fileType: String, val displayName: String) : FileNavigationEvent
}

@HiltViewModel
class FileViewModel @Inject constructor(
    private val documentUseCases: DocumentUseCases
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