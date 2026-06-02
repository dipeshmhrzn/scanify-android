package com.scanify.app.domain.usecases.themeusecases

import com.scanify.app.domain.repository.ThemePreferenceRepository
import com.scanify.app.ui.theme.ThemeMode
import javax.inject.Inject

class UpdateThemeModeUseCase @Inject constructor(
    private val repository: ThemePreferenceRepository
) {
    suspend operator fun invoke(mode : ThemeMode){
        repository.updateThemeMode(mode)
    }
}