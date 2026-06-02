package com.scanify.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanify.app.domain.usecases.themeusecases.GetThemeModeUseCase
import com.scanify.app.domain.usecases.themeusecases.UpdateThemeModeUseCase
import com.scanify.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    private val updateThemeModeUseCase: UpdateThemeModeUseCase
): ViewModel() {

    val currentThemeMode: StateFlow<ThemeMode> = getThemeModeUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
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
}