package com.scanify.app.presentation.viewmodels

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.usecases.DocumentUseCases
import com.scanify.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface PreviewUiState {
    data object Loading : PreviewUiState
    data object Error : PreviewUiState
    data class Success(
        val document: Document,
        val pageCount: Int,
        val lastModified: Long,
        val isUpdating: Boolean = false // Prevents the UI from flashing during appends
    ) : PreviewUiState
}

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val documentUseCases: DocumentUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val previewRoute = savedStateHandle.toRoute<Routes.PreviewScreen>()
    private val documentId: Long = previewRoute.id

    private val _isUpdating = MutableStateFlow(false)

    val uiState: StateFlow<PreviewUiState> = documentUseCases.getDocuments()
        .map { documents ->
            val document = documents.find { it.id == documentId }

            if (document == null) {
                PreviewUiState.Error
            } else {
                val file = File(document.filePath)
                val lastModified = if (file.exists()) file.lastModified() else 0L

                val pageCount = if (document.fileType.uppercase() == "PDF" && file.exists()) {
                    var count = 0
                    try {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                            PdfRenderer(pfd).use { renderer -> count = renderer.pageCount }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    count
                } else {
                    1
                }

                PreviewUiState.Success(
                    document = document,
                    pageCount = pageCount,
                    lastModified = lastModified,
                    isUpdating = _isUpdating.value
                )
            }
        }
        .combine(_isUpdating) { state, updating ->
            if (state is PreviewUiState.Success) state.copy(isUpdating = updating) else state
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PreviewUiState.Loading
        )
    fun appendScannedImages(imageUris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            documentUseCases.appendImagesToDocument(documentId, imageUris)
                .onFailure {
                    _isUpdating.value = false
                }
            _isUpdating.value = false
        }
    }

}