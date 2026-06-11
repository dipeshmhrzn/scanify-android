package com.scanify.app.presentation.components

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext

@Composable
fun DoubleBackToExit(
    enabled: Boolean,
    message: String = "Press back again to exit"
) {
    val context = LocalContext.current

    var lastBackPressedTime by rememberSaveable {
        mutableLongStateOf(0L)
    }

    BackHandler(enabled = enabled) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastBackPressedTime < 2000) {
            (context as? Activity)?.moveTaskToBack(true)
        } else {
            lastBackPressedTime = currentTime

            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}