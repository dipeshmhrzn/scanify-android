package com.scanify.app.presentation

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.scanify.app.presentation.notification.NotificationHelper
import com.scanify.app.presentation.util.DocumentPageFetcher
import com.scanify.app.presentation.util.DocumentPageKeyer
import com.scanify.app.presentation.worker.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ScanifyApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        ReminderScheduler.scheduleDailyReminder(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(DocumentPageKeyer())
                add(DocumentPageFetcher.Factory())
            }
            .build()
    }
}