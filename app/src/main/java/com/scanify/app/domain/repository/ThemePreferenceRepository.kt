package com.scanify.app.domain.repository

import com.scanify.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferenceRepository {

    val themeMode: Flow<ThemeMode>

    suspend fun updateThemeMode(mode: ThemeMode)

}