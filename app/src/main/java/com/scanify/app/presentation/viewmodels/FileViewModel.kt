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