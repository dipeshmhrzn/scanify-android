package com.scanify.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface OnboardingPreferenceRepository {
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
}