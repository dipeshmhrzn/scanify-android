package com.scanify.app.presentation.worker

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

object LastActivityTracker {
    private val LAST_ACTIVE_DAY_KEY = longPreferencesKey("last_active_epoch_day")

    suspend fun markActiveToday(dataStore: DataStore<Preferences>) {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        dataStore.edit { prefs -> prefs[LAST_ACTIVE_DAY_KEY] = today }
    }

    suspend fun wasActiveToday(dataStore: DataStore<Preferences>): Boolean {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val lastActive = dataStore.data.first()[LAST_ACTIVE_DAY_KEY] ?: -1L
        return lastActive == today
    }
}