package com.scanify.app.presentation

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.scanify.app.presentation.util.DocumentPageFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScanifyApp : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(DocumentPageFetcher.Factory())
            }
            .build()
    }
}