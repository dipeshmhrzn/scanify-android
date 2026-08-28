package com.scanify.app.presentation.worker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.scanify.app.presentation.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val wasActiveToday = LastActivityTracker.wasActiveToday(dataStore)
        if (!wasActiveToday) {
            NotificationHelper.showReminderNotification(applicationContext)
        }
        return Result.success()
    }
}