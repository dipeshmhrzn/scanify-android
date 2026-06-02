package com.scanify.app.presentation.viewmodels

//sealed interface PreviewUiState {
//    object Loading : PreviewUiState
//    object Error : PreviewUiState
//    data class Success(
//        val document: Document,
//        val pdfPages: List<Bitmap> = emptyList()
//    ) : PreviewUiState
//}
//
//@HiltViewModel
//class PreviewViewModel @Inject constructor(
//    private val documentUseCases: DocumentUseCases,
//    savedStateHandle: SavedStateHandle
//) : ViewModel() {
//
//    private val previewRoute = savedStateHandle.toRoute<Routes.PreviewScreen>()
//    private val documentId: Long = previewRoute.id
//
//    private val _uiState = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
//    val uiState = _uiState.asStateFlow()
//
//
//    init {
//        loadDocumentState()
//    }
//
//    fun loadDocumentState() {
//        viewModelScope.launch(Dispatchers.IO) {
//            val startTime = System.currentTimeMillis()
//            _uiState.value = PreviewUiState.Loading
//
//            val document = documentUseCases.getDocumentById(documentId)
//
//            if (document == null) {
//                enforceMinimumDelay(startTime)
//                _uiState.value = PreviewUiState.Error
//                return@launch
//            }
//
//            val pages: List<Bitmap> = if (document.fileType.uppercase() == "PDF") {
//                PdfPageRenderer.renderAllPages(document.filePath)
//            } else {
//                val bitmap: Bitmap? = BitmapFactory.decodeFile(document.filePath)
//                if (bitmap != null) listOf(bitmap) else emptyList()
//            }
//
//            enforceMinimumDelay(startTime)
//            _uiState.value = PreviewUiState.Success(document = document, pdfPages = pages)
//        }
//    }
//
//    fun appendScannedImages(imageUris: List<String>) {
//        viewModelScope.launch(Dispatchers.IO) {
//            _uiState.value = PreviewUiState.Loading
//            documentUseCases.appendImagesToDocument(documentId, imageUris)
//                .onSuccess {
//                    loadDocumentState()
//                }
//                .onFailure {
//                    _uiState.value = PreviewUiState.Error
//                }
//        }
//    }
//}

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.usecases.DocumentUseCases
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.util.enforceMinimumDelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadDocumentState(isInitialLoad = true)
    }

    private fun loadDocumentState(isInitialLoad: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            if (isInitialLoad) {
                _uiState.value = PreviewUiState.Loading
            } else {
                (_uiState.value as? PreviewUiState.Success)?.let { currentSuccess ->
                    _uiState.value = currentSuccess.copy(isUpdating = true)
                }
            }

            val document = documentUseCases.getDocumentById(documentId)
            if (document == null) {
                if (isInitialLoad) enforceMinimumDelay(startTime)
                _uiState.value = PreviewUiState.Error
                return@launch
            }

            val file = File(document.filePath)
            val lastModified = if (file.exists()) file.lastModified() else 0L

            // Calculate total pages quickly without rendering the images
            val pageCount = if (document.fileType.uppercase() == "PDF" && file.exists()) {
                var count = 0
                try {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer -> count = renderer.pageCount }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                count
            } else {
                1 // Images (JPG/PNG) act as a 1-page document
            }

            if (isInitialLoad) enforceMinimumDelay(startTime)

            _uiState.value = PreviewUiState.Success(
                document = document,
                pageCount = pageCount,
                lastModified = lastModified,
                isUpdating = false
            )
        }
    }

    fun appendScannedImages(imageUris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            (_uiState.value as? PreviewUiState.Success)?.let { currentSuccess ->
                _uiState.value = currentSuccess.copy(isUpdating = true)
            } ?: run {
                _uiState.value = PreviewUiState.Loading
            }

            documentUseCases.appendImagesToDocument(documentId, imageUris)
                .onSuccess { loadDocumentState(isInitialLoad = false) }
                .onFailure { _uiState.value = PreviewUiState.Error }
        }
    }
}