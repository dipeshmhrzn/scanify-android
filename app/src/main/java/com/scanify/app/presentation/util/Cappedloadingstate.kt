package com.scanify.app.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberCappedLoadingState(
    isActive: Boolean,
    capMillis: Long = 5000L,
    onCapReached: () -> Unit = {}
): Boolean {
    var showDialog by remember { mutableStateOf(false) }
    var capNotified by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            showDialog = true
            capNotified = false
            delay(capMillis.milliseconds)
            if (isActive) {
                showDialog = false
                if (!capNotified) {
                    capNotified = true
                    onCapReached()
                }
            }
        } else {
            showDialog = false
        }
    }

    return showDialog
}