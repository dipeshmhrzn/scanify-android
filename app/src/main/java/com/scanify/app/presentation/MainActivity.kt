package com.scanify.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanify.app.navigation.Navigation
import com.scanify.app.presentation.util.UpdateManager
import com.scanify.app.presentation.viewmodels.MainViewModel
import com.scanify.app.ui.theme.ScanifyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject
    lateinit var updateManager: UpdateManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // User rejected the update prompt dialog; safely handle or log
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value
        }
        enableEdgeToEdge()

        updateManager.registerUpdateListener()

        if (savedInstanceState == null) {
            updateManager.checkForUpdates(updateLauncher)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

            ScanifyTheme(themeMode = themeMode) {
                if (!isLoading) {
                    Navigation(
                        isCompleted = isCompleted,
                        updateManager = updateManager,
                        onOnboardingFinished = { viewModel.completeOnboarding() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::updateManager.isInitialized) {
            updateManager.syncManagerOnResume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::updateManager.isInitialized) {
            updateManager.unregisterUpdateListener()
        }
    }
}