package com.scanify.app.presentation.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.scanify.app.domain.model.ExportState
import com.scanify.app.domain.usecases.getdocumentusecases.GetDocumentsUseCase
import com.scanify.app.domain.usecases.themeusecases.GetThemeModeUseCase
import com.scanify.app.domain.usecases.themeusecases.UpdateThemeModeUseCase
import com.scanify.app.presentation.worker.BackupWorker
import com.scanify.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    private val updateThemeModeUseCase: UpdateThemeModeUseCase,
    private val getDocumentsUseCase: GetDocumentsUseCase,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val currentThemeMode: StateFlow<ThemeMode> = getThemeModeUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    private val _exportUiState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportUiState: StateFlow<ExportState> = _exportUiState.asStateFlow()

    val documentCount: StateFlow<Int> = getDocumentsUseCase()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val workManager = WorkManager.getInstance(appContext)

    fun cycleTheme() {
        viewModelScope.launch {
            val current = currentThemeMode.value
            val next = when (current) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
            updateThemeModeUseCase(next)
        }
    }

    fun triggerFullBackupExport(targetUri: Uri? = null) {
        val inputData = Data.Builder().apply {
            targetUri?.let { putString(BackupWorker.KEY_TARGET_URI, it.toString()) }
        }.build()

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(inputData)
            .build()

        _exportUiState.value = ExportState.Processing(0f, "Preparing backup...")
        workManager.enqueue(request)

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val destinationPath = workInfo.outputData.getString(BackupWorker.KEY_RESULT_MESSAGE)
                            ?: "your device"
                        _exportUiState.value = ExportState.Success(destinationPath)
                    }
                    WorkInfo.State.FAILED -> {
                        val message = workInfo.outputData.getString(BackupWorker.KEY_RESULT_MESSAGE)
                            ?: "Backup failed."
                        _exportUiState.value = ExportState.Error(Exception(message))
                    }
                    WorkInfo.State.CANCELLED -> {
                        _exportUiState.value = ExportState.Idle
                    }
                    else -> {
                        if (_exportUiState.value !is ExportState.Success && _exportUiState.value !is ExportState.Error) {
                            val fraction = workInfo?.progress?.getFloat(BackupWorker.KEY_PROGRESS_FRACTION, 0f) ?: 0f
                            val label = workInfo?.progress?.getString(BackupWorker.KEY_PROGRESS_LABEL) ?: "Backing up..."
                            _exportUiState.value = ExportState.Processing(fraction, label)
                        }
                    }
                }
            }
        }
    }

    fun resetExportState() {
        _exportUiState.value = ExportState.Idle
    }
}