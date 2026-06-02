package com.scanify.app.domain.usecases.themeusecases

import com.scanify.app.domain.repository.ThemePreferenceRepository
import com.scanify.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val repository: ThemePreferenceRepository
) {
    operator fun invoke(): Flow<ThemeMode>{
        return repository.themeMode
    }
}