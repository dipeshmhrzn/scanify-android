package com.scanify.app.presentation.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.scanify.app.data.backup.ExportStorageManager
import com.scanify.app.domain.model.ExportState
import com.scanify.app.presentation.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val exportStorageManager: ExportStorageManager
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TARGET_URI = "target_uri"
        const val KEY_RESULT_MESSAGE = "result_message"
        const val KEY_PROGRESS_FRACTION = "progress_fraction"
        const val KEY_PROGRESS_LABEL = "progress_label"
    }

    override suspend fun doWork(): Result {
        val targetUriString = inputData.getString(KEY_TARGET_URI)
        val targetUri = targetUriString?.toUri()

        setForeground(createForegroundInfo(0, "Preparing backup..."))

        var lastNotifiedPercent = -1
        var lastNotifyTimeMs = 0L
        var lastState: ExportState = ExportState.Idle

        exportStorageManager.executeFullExport(targetUri).collect { state ->
            lastState = state
            if (state is ExportState.Processing) {
                val percent = (state.progress * 100).toInt()
                val roundedPercent = (percent / 5) * 5
                val now = System.currentTimeMillis()
                val steppedEnough = roundedPercent != lastNotifiedPercent
                val enoughTimePassed = (now - lastNotifyTimeMs) >= 500L

                if (steppedEnough && enoughTimePassed) {
                    lastNotifiedPercent = roundedPercent
                    lastNotifyTimeMs = now
                    setForeground(
                        createForegroundInfo(roundedPercent, "Backing up: ${state.currentFileName}")
                    )
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS_FRACTION to state.progress,
                            KEY_PROGRESS_LABEL to state.currentFileName
                        )
                    )
                }
            }
        }

        (lastState as? ExportState.Processing)?.let { finalProcessing ->
            val finalPercent = (finalProcessing.progress * 100).toInt()
            setForeground(createForegroundInfo(finalPercent, "Backing up: ${finalProcessing.currentFileName}"))
            setProgress(
                workDataOf(
                    KEY_PROGRESS_FRACTION to finalProcessing.progress,
                    KEY_PROGRESS_LABEL to finalProcessing.currentFileName
                )
            )
        }

        val finalState = lastState

        delay(400.milliseconds)

        return when (finalState) {
            is ExportState.Success -> {
                NotificationHelper.showCompletionNotification(
                    applicationContext,
                    "Backup complete",
                    "Saved to ${finalState.destinationPath}",
                    isSuccess = true
                )
                Result.success(
                    workDataOf(KEY_RESULT_MESSAGE to finalState.destinationPath)
                )
            }
            is ExportState.Error -> {
                val errorMessage = finalState.throwable.localizedMessage ?: "Unknown error"
                NotificationHelper.showCompletionNotification(
                    applicationContext,
                    "Backup failed",
                    errorMessage,
                    isSuccess = false
                )
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to errorMessage))
            }
            else -> Result.failure()
        }
    }

    private fun createForegroundInfo(progressPercent: Int, statusText: String): ForegroundInfo {
        val notification = NotificationHelper.buildProgressNotification(
            applicationContext,
            title = statusText,
            progressPercent = progressPercent
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
        }
    }
}