package com.scanify.app.presentation.util

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.scanify.app.domain.model.InAppUpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) {
    private val _updateState = MutableStateFlow<InAppUpdateState>(InAppUpdateState.Idle)
    val updateState: StateFlow<InAppUpdateState> = _updateState.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                if (totalBytesToDownload > 0) {
                    val percent = ((bytesDownloaded * 100) / totalBytesToDownload).toInt()
                    _updateState.value = InAppUpdateState.Downloading(percent)
                }
            }
            InstallStatus.DOWNLOADED -> _updateState.value = InAppUpdateState.Downloaded
            InstallStatus.FAILED -> _updateState.value = InAppUpdateState.Failed
            else -> { /* Handle other minor states if necessary */ }
        }
    }

    fun registerUpdateListener() {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    fun unregisterUpdateListener() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    fun checkForUpdates(updateLauncher: ActivityResultLauncher<IntentSenderRequest>) {

        if (_updateState.value is InAppUpdateState.Downloading || _updateState.value is InAppUpdateState.Downloaded) return

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                } catch (e: Exception) {
                    _updateState.value = InAppUpdateState.Failed
                }
            }
        }
    }

    fun syncManagerOnResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _updateState.value = InAppUpdateState.Downloaded
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADING) {
                val total = appUpdateInfo.totalBytesToDownload()
                if (total > 0) {
                    val percent = ((appUpdateInfo.bytesDownloaded() * 100) / total).toInt()
                    _updateState.value = InAppUpdateState.Downloading(percent)
                }
            }
        }
    }

    fun completeUpdateInstallation() {
        appUpdateManager.completeUpdate()
    }
}