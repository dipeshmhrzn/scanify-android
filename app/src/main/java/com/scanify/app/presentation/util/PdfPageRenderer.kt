package com.scanify.app.presentation.util

import android.graphics.Bitmap
import android.graphics.Canvas  // Import Canvas
import android.graphics.Color   // Import Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.createBitmap

object PdfPageRenderer {

    suspend fun renderAllPages(filePath: String): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        val file = File(filePath)
        if (!file.exists()) return@withContext emptyList()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val bitmap = createBitmap(page.width * 2, page.height * 2)

                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            renderer?.close()
            pfd?.close()
        }
        bitmaps
    }
}