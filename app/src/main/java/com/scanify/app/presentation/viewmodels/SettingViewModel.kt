package com.scanify.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanify.app.data.backup.ExportStorageManager
import com.scanify.app.domain.model.ExportState
import com.scanify.app.domain.usecases.getdocumentusecases.GetDocumentsUseCase
import com.scanify.app.domain.usecases.themeusecases.GetThemeModeUseCase
import com.scanify.app.domain.usecases.themeusecases.UpdateThemeModeUseCase
import com.scanify.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val exportStorageManager: ExportStorageManager,
    private val getDocumentsUseCase: GetDocumentsUseCase
): ViewModel() {

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


    fun triggerFullBackupExport() {
        viewModelScope.launch {
            exportStorageManager.executeFullExport().collect { state ->
                _exportUiState.value = state
            }
        }
    }

    fun resetExportState() {
        _exportUiState.value = ExportState.Idle
    }
}