package com.scanify.app.presentation.worker

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.scanify.app.data.repositoryimpl.DocumentSaveManager
import com.scanify.app.domain.usecases.DocumentUseCases
import com.scanify.app.presentation.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FileTaskWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val documentUseCases: DocumentUseCases,
    private val saveManager: DocumentSaveManager,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_TYPE = "task_type"
        const val KEY_URIS = "uris"
        const val KEY_DOCUMENT_ID = "document_id"
        const val KEY_TARGET_URI = "target_uri"
        const val KEY_RESULT_MESSAGE = "result_message"
        const val KEY_NAVIGATE_DOCUMENT_ID = "navigate_document_id"

        const val TASK_IMPORT_FILES = "import_files"
        const val TASK_IMPORT_IMAGES = "import_images"
        const val TASK_SAVE_TO_DEVICE = "save_to_device"
        const val TASK_SAVE_TO_URI = "save_to_uri"
        const val TASK_SAVE_IMAGES = "save_images"
        const val TASK_SAVE_IMAGES_LEGACY = "save_images_legacy"
    }

    override suspend fun doWork(): Result {
        val taskType = inputData.getString(KEY_TASK_TYPE) ?: return Result.failure()
        setForeground(createForegroundInfo(taskLabel(taskType)))

        return try {
            when (taskType) {
                TASK_IMPORT_FILES -> runImportFiles()
                TASK_IMPORT_IMAGES -> runImportImages()
                TASK_SAVE_TO_DEVICE -> runSaveToDevice()
                TASK_SAVE_TO_URI -> runSaveToUri()
                TASK_SAVE_IMAGES -> runSaveImages()
                TASK_SAVE_IMAGES_LEGACY -> runSaveImagesLegacy()
                else -> Result.failure()
            }
        } catch (e: Exception) {
            notifyFailure(taskType, e.localizedMessage ?: "Operation failed.")
            Result.failure(workDataOf(KEY_RESULT_MESSAGE to (e.localizedMessage ?: "Operation failed.")))
        }
    }

    private suspend fun runImportFiles(): Result {
        val uris = inputData.getStringArray(KEY_URIS)?.toList() ?: return Result.failure()
        val outcome = documentUseCases.importMultipleFiles(uris)
        return outcome.fold(
            onSuccess = { summary ->
                LastActivityTracker.markActiveToday(dataStore)
                val message = if (summary.failures.isEmpty()) {
                    "Imported ${summary.successfulIds.size} file(s)."
                } else {
                    "Imported ${summary.successfulIds.size} file(s), ${summary.failures.size} failed."
                }
                notifySuccess("Import complete", message)
                Result.success(workDataOf(KEY_RESULT_MESSAGE to message))
            },
            onFailure = { err ->
                val message = err.localizedMessage ?: "Import failed."
                notifyFailure("Import failed", message)
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to message))
            }
        )
    }

    private suspend fun runImportImages(): Result {
        val uris = inputData.getStringArray(KEY_URIS)?.toList() ?: return Result.failure()
        val outcome = documentUseCases.importMultipleImages(uris)
        return outcome.fold(
            onSuccess = { documentId ->
                LastActivityTracker.markActiveToday(dataStore)
                notifySuccess("Import complete", "Scan saved.")
                Result.success(
                    workDataOf(
                        KEY_RESULT_MESSAGE to "Scan saved.",
                        KEY_NAVIGATE_DOCUMENT_ID to documentId
                    )
                )
            },
            onFailure = { err ->
                val message = err.localizedMessage ?: "Import failed."
                notifyFailure("Import failed", message)
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to message))
            }
        )
    }

    private suspend fun runSaveToDevice(): Result {
        val documentId = inputData.getLong(KEY_DOCUMENT_ID, -1L)
        val document = documentUseCases.getDocumentById(documentId) ?: return Result.failure()
        return when (val outcome = saveManager.autoSaveToDocuments(document)) {
            is DocumentSaveManager.SaveOutcome.Message -> {
                notifySuccess("Save complete", outcome.text)
                Result.success(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
            is DocumentSaveManager.SaveOutcome.Failure -> {
                notifyFailure("Save failed", outcome.text)
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
        }
    }

    private suspend fun runSaveToUri(): Result {
        val documentId = inputData.getLong(KEY_DOCUMENT_ID, -1L)
        val targetUriString = inputData.getString(KEY_TARGET_URI) ?: return Result.failure()
        val document = documentUseCases.getDocumentById(documentId) ?: return Result.failure()
        return when (val outcome = saveManager.saveToUri(document, targetUriString.toUri())) {
            is DocumentSaveManager.SaveOutcome.Message -> {
                notifySuccess("Save complete", outcome.text)
                Result.success(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
            is DocumentSaveManager.SaveOutcome.Failure -> {
                notifyFailure("Save failed", outcome.text)
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
        }
    }

    private suspend fun runSaveImages(): Result {
        val documentId = inputData.getLong(KEY_DOCUMENT_ID, -1L)
        val document = documentUseCases.getDocumentById(documentId) ?: return Result.failure()
        return when (val outcome = saveManager.saveImagesToDevice(document)) {
            is DocumentSaveManager.SaveOutcome.Message -> {
                notifySuccess("Save complete", outcome.text)
                Result.success(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
            is DocumentSaveManager.SaveOutcome.Failure -> {
                notifyFailure("Save failed", outcome.text)
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
        }
    }

    private suspend fun runSaveImagesLegacy(): Result {
        val documentId = inputData.getLong(KEY_DOCUMENT_ID, -1L)
        val targetUriString = inputData.getString(KEY_TARGET_URI) ?: return Result.failure()
        val document = documentUseCases.getDocumentById(documentId) ?: return Result.failure()
        return when (val outcome = saveManager.saveImagesToDeviceLegacy(document,
            targetUriString.toUri())) {
            is DocumentSaveManager.SaveOutcome.Message -> {
                notifySuccess("Save complete", outcome.text)
                Result.success(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
            is DocumentSaveManager.SaveOutcome.Failure -> {
                notifyFailure("Save failed", outcome.text)
                Result.failure(workDataOf(KEY_RESULT_MESSAGE to outcome.text))
            }
        }
    }

    private fun taskLabel(taskType: String): String = when (taskType) {
        TASK_IMPORT_FILES, TASK_IMPORT_IMAGES -> "Importing..."
        TASK_SAVE_TO_DEVICE, TASK_SAVE_TO_URI -> "Saving document..."
        TASK_SAVE_IMAGES, TASK_SAVE_IMAGES_LEGACY -> "Saving images..."
        else -> "Working..."
    }

    private suspend fun notifySuccess(title: String, message: String) =
        NotificationHelper.showCompletionNotification(applicationContext, title, message, isSuccess = true)

    private suspend fun notifyFailure(title: String, message: String) =
        NotificationHelper.showCompletionNotification(applicationContext, title, message, isSuccess = false)

    private fun createForegroundInfo(statusText: String): ForegroundInfo {
        val notification = NotificationHelper.buildProgressNotification(
            applicationContext,
            title = statusText,
            progressPercent = 0,
            indeterminate = true
        )
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.PROGRESS_NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
        }
    }
}