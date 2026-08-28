package com.scanify.app.presentation.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.scanify.app.domain.model.Document
import com.scanify.app.domain.usecases.DocumentUseCases
import com.scanify.app.presentation.worker.FileTaskWorker
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
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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
                delay(300.milliseconds)
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

    private val workManager = WorkManager.getInstance(appContext)

    fun onDocumentClick(document: Document) {
        viewModelScope.launch {
            routeDocument(document)
        }
    }

    fun importMultipleFiles(uriStrings: List<String>) {
        if (uriStrings.isEmpty()) return
        val inputData = Data.Builder()
            .putString(FileTaskWorker.KEY_TASK_TYPE, FileTaskWorker.TASK_IMPORT_FILES)
            .putStringArray(FileTaskWorker.KEY_URIS, uriStrings.toTypedArray())
            .build()

        enqueueFileTask(
            inputData = inputData,
            trackingFlow = _isImporting,
            startMessage = "File import started.",
            onSuccess = { outputData ->
                outputData.getString(FileTaskWorker.KEY_RESULT_MESSAGE)?.let { message ->
                    _navigationEvent.send(FileNavigationEvent.ShowMessage(message))
                }
            }
        )
    }

    fun importMultipleImages(uriStrings: List<String>) {
        if (uriStrings.isEmpty()) return
        val inputData = Data.Builder()
            .putString(FileTaskWorker.KEY_TASK_TYPE, FileTaskWorker.TASK_IMPORT_IMAGES)
            .putStringArray(FileTaskWorker.KEY_URIS, uriStrings.toTypedArray())
            .build()

        enqueueFileTask(
            inputData = inputData,
            trackingFlow = _isImporting,
            startMessage = "Image import started.",
            onSuccess = { outputData ->
                val documentId = outputData.getLong(FileTaskWorker.KEY_NAVIGATE_DOCUMENT_ID, -1L)
                if (documentId != -1L) {
                    _navigationEvent.send(FileNavigationEvent.NavigateToPreview(documentId))
                }
            }
        )
    }

    fun handleScannedDocuments(imageUris: List<String>) = importMultipleImages(imageUris)

    fun renameDocument(document: Document, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty() || trimmedName.equals(document.name, ignoreCase = true)) return

        viewModelScope.launch {
            documentUseCases.renameDocumentUseCase(document, trimmedName)
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
        val inputData = Data.Builder()
            .putString(FileTaskWorker.KEY_TASK_TYPE, FileTaskWorker.TASK_SAVE_TO_DEVICE)
            .putLong(FileTaskWorker.KEY_DOCUMENT_ID, document.id)
            .build()
        enqueueSaveTask(inputData)
    }

    fun saveToSelectedUri(document: Document, targetUri: Uri) {
        val inputData = Data.Builder()
            .putString(FileTaskWorker.KEY_TASK_TYPE, FileTaskWorker.TASK_SAVE_TO_URI)
            .putLong(FileTaskWorker.KEY_DOCUMENT_ID, document.id)
            .putString(FileTaskWorker.KEY_TARGET_URI, targetUri.toString())
            .build()
        enqueueSaveTask(inputData)
    }

    fun saveImagesToDevice(document: Document) {
        val inputData = Data.Builder()
            .putString(FileTaskWorker.KEY_TASK_TYPE, FileTaskWorker.TASK_SAVE_IMAGES)
            .putLong(FileTaskWorker.KEY_DOCUMENT_ID, document.id)
            .build()
        enqueueSaveTask(inputData)
    }

    fun saveImagesToDeviceLegacy(document: Document, treeUri: Uri) {
        val inputData = Data.Builder()
            .putString(FileTaskWorker.KEY_TASK_TYPE, FileTaskWorker.TASK_SAVE_IMAGES_LEGACY)
            .putLong(FileTaskWorker.KEY_DOCUMENT_ID, document.id)
            .putString(FileTaskWorker.KEY_TARGET_URI, treeUri.toString())
            .build()
        enqueueSaveTask(inputData)
    }

    private fun enqueueSaveTask(inputData: Data) {
        enqueueFileTask(
            inputData = inputData,
            trackingFlow = _isSaving,
            startMessage = "Save started.",
            onSuccess = { outputData ->
                outputData.getString(FileTaskWorker.KEY_RESULT_MESSAGE)?.let { message ->
                    _navigationEvent.send(FileNavigationEvent.ShowMessage(message))
                }
            }
        )
    }

    private fun enqueueFileTask(
        inputData: Data,
        trackingFlow: MutableStateFlow<Boolean>,
        startMessage: String,
        onSuccess: suspend (androidx.work.Data) -> Unit
    ) {
        val request = OneTimeWorkRequestBuilder<FileTaskWorker>()
            .setInputData(inputData)
            .build()

        trackingFlow.value = true
        workManager.enqueue(request)

        viewModelScope.launch {

            _navigationEvent.send(FileNavigationEvent.ShowMessage(startMessage))

            workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        trackingFlow.value = false
                        onSuccess(workInfo.outputData)
                    }
                    WorkInfo.State.FAILED -> {
                        trackingFlow.value = false
                        val message = workInfo.outputData.getString(FileTaskWorker.KEY_RESULT_MESSAGE)
                            ?: "Operation failed."
                        _navigationEvent.send(FileNavigationEvent.ShowError(message))
                    }
                    WorkInfo.State.CANCELLED -> {
                        trackingFlow.value = false
                    }
                    else -> {

                    }
                }
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