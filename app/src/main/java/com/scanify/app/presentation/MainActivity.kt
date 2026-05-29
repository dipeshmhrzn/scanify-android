package com.scanify.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanify.app.navigation.Navigation
import com.scanify.app.presentation.viewmodels.MainViewModel
import com.scanify.app.ui.theme.ScanifyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            enableEdgeToEdge()

            ScanifyTheme(themeMode = themeMode) {
                Navigation()
            }
        }
    }
}