package com.scanify.app.presentation.util

import kotlinx.coroutines.delay

suspend fun enforceMinimumDelay(startTime: Long) {
    val elapsedTime = System.currentTimeMillis() - startTime
    if (elapsedTime < 300L) {
        delay(300L - elapsedTime)
    }
}