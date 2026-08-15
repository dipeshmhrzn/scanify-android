package com.scanify.app.presentation.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class DocumentPageRequest(
    val filePath: String,
    val pageIndex: Int,
    val lastModified: Long
)

class DocumentPageKeyer : Keyer<DocumentPageRequest> {
    override fun key(data: DocumentPageRequest, options: Options): String {
        return "pdf_thumb:${data.filePath}_page:${data.pageIndex}_mod:${data.lastModified}_size:${options.size}"
    }
}

class DocumentPageFetcher(
    private val data: DocumentPageRequest,
    private val options: Options
) : Fetcher {

    companion object {
        private val locksByPath = ConcurrentHashMap<String, Mutex>()
        private fun lockFor(path: String): Mutex = locksByPath.getOrPut(path) { Mutex() }
    }

    override suspend fun fetch(): FetchResult? {
        val file = File(data.filePath)
        if (!file.exists()) return null

        val targetWidth = options.size.width.pxOrElse { -1 }
        val targetHeight = options.size.height.pxOrElse { -1 }

        val bitmap = lockFor(data.filePath).withLock {
            renderSinglePdfPage(file, data.pageIndex, targetWidth, targetHeight)
        } ?: return null

        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    private fun renderSinglePdfPage(file: File, pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            if (pageIndex >= renderer.pageCount) return null

            renderer.openPage(pageIndex).use { page ->
                val scale = if (targetWidth > 0 && targetHeight > 0) {
                    val widthScale = targetWidth.toFloat() / page.width
                    val heightScale = targetHeight.toFloat() / page.height
                    minOf(widthScale, heightScale)
                } else {
                    1.0f
                }

                val finalWidth = (page.width * scale).toInt().coerceAtLeast(1)
                val finalHeight = (page.height * scale).toInt().coerceAtLeast(1)

                val bitmap = createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        } catch (e: Throwable) {
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