package com.scanify.app.presentation.util

//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.pdf.PdfRenderer
//import android.os.ParcelFileDescriptor
//import coil3.ImageLoader
//import coil3.asImage
//import coil3.decode.DataSource
//import coil3.fetch.FetchResult
//import coil3.fetch.Fetcher
//import coil3.fetch.ImageFetchResult
//import coil3.request.Options
//import java.io.File
//import androidx.core.graphics.createBitmap
//
//data class DocumentPageRequest(
//    val filePath: String,
//    val pageIndex: Int,
//    val lastModified: Long
//)
//
//class DocumentPageFetcher(
//    private val data: DocumentPageRequest,
//    private val options: Options
//) : Fetcher {
//
//    override suspend fun fetch(): FetchResult? {
//        val file = File(data.filePath)
//        if (!file.exists()) return null
//
//        val bitmap = renderSinglePdfPage(file, data.pageIndex) ?: return null
//
//        return ImageFetchResult(
//            image = bitmap.asImage(),
//            isSampled = false,
//            dataSource = DataSource.DISK
//        )
//    }
//
//    private fun renderSinglePdfPage(file: File, pageIndex: Int): Bitmap? {
//        var pfd: ParcelFileDescriptor? = null
//        var renderer: PdfRenderer? = null
//
//        return try {
//            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
//            renderer = PdfRenderer(pfd)
//
//            if (pageIndex >= renderer.pageCount) return null
//
//            renderer.openPage(pageIndex).use { page ->
//                val bitmap = createBitmap(page.width * 2, page.height * 2)
//                val canvas = Canvas(bitmap)
//                canvas.drawColor(Color.WHITE)
//
//                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
//                bitmap
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        } finally {
//            renderer?.close()
//            pfd?.close()
//        }
//    }
//
//    class Factory : Fetcher.Factory<DocumentPageRequest> {
//        override fun create(data: DocumentPageRequest, options: Options, imageLoader: ImageLoader): Fetcher {
//            return DocumentPageFetcher(data, options)
//        }
//    }
//}

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import androidx.core.graphics.createBitmap

data class DocumentPageRequest(
    val filePath: String,
    val pageIndex: Int,
    val lastModified: Long
)

class DocumentPageFetcher(
    private val data: DocumentPageRequest,
    private val options: Options
) : Fetcher {

    companion object {
        // A Mutex (Mutual Exclusion) ensures that no matter how fast the user scrolls,
        // only ONE background thread can interact with PdfRenderer at a time.
        // The other requests will wait patiently in line without crashing the OS.
        private val renderMutex = Mutex()
    }

    override suspend fun fetch(): FetchResult? {
        val file = File(data.filePath)
        if (!file.exists()) return null

        // Suspend and wait in line until the lock is free
        val bitmap = renderMutex.withLock {
            renderSinglePdfPage(file, data.pageIndex)
        } ?: return null

        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    private fun renderSinglePdfPage(file: File, pageIndex: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            if (pageIndex >= renderer.pageCount) return null

            renderer.openPage(pageIndex).use { page ->
                // Dropped from 2f to 1.5f.
                // 1.5x is the sweet spot for razor-sharp text without triggering Out-Of-Memory crashes on 20+ page PDFs.
                val scale = 1.5f
                val bitmap =
                    createBitmap((page.width * scale).toInt(), (page.height * scale).toInt())

                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        } catch (e: Throwable) {
            // CRITICAL: We changed 'Exception' to 'Throwable'.
            // OutOfMemoryError is an Error, not an Exception. This ensures it doesn't crash your app silently.
            e.printStackTrace()
            null
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    class Factory : Fetcher.Factory<DocumentPageRequest> {
        override fun create(data: DocumentPageRequest, options: Options, imageLoader: ImageLoader): Fetcher {
            return DocumentPageFetcher(data, options)
        }
    }
}