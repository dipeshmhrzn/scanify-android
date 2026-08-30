package com.scanify.app.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.scanify.app.presentation.worker.FileTaskWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdCardViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _navigationEvent = Channel<FileNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val workManager = WorkManager.getInstance(appContext)

    fun generatePdf(frontUri: String, backUri: String?) {
        val inputData = Data.Builder()
            .putString(FileTaskWorker.KEY_TASK_TYPE, FileTaskWorker.TASK_GENERATE_ID_CARD)
            .putString(FileTaskWorker.KEY_ID_CARD_FRONT_URI, frontUri)
            .apply { backUri?.let { putString(FileTaskWorker.KEY_ID_CARD_BACK_URI, it) } }
            .build()

        val request = OneTimeWorkRequestBuilder<FileTaskWorker>()
            .setInputData(inputData)
            .build()

        _isSaving.value = true
        workManager.enqueue(request)

        viewModelScope.launch {
            _navigationEvent.send(
                FileNavigationEvent.ShowMessage("Generating ID card PDF ...")
            )

            workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _isSaving.value = false
                        val documentId = workInfo.outputData.getLong(FileTaskWorker.KEY_NAVIGATE_DOCUMENT_ID, -1L)
                        if (documentId != -1L) {
                            _navigationEvent.send(FileNavigationEvent.NavigateToPreview(documentId))
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        _isSaving.value = false
                        val message = workInfo.outputData.getString(FileTaskWorker.KEY_RESULT_MESSAGE)
                            ?: "Failed to generate the ID card PDF."
                        _navigationEvent.send(FileNavigationEvent.ShowError(message))
                    }
                    WorkInfo.State.CANCELLED -> {
                        _isSaving.value = false
                    }
                    else -> {
                        // RUNNING/ENQUEUED - the notification (posted by FileTaskWorker) is
                        // the progress indicator; nothing further to do here.
                    }
                }
            }
        }
    }
}