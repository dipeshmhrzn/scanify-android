package com.scanify.app.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ImageOptimizer {

    const val MAX_PAGE_DIMEN = 1600

    suspend fun decodeScaledBitmap(file: File, maxDimen: Int = MAX_PAGE_DIMEN): Bitmap? =
        withContext(Dispatchers.Default) {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return@withContext null

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize =
                    calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxDimen)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampled = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                ?: return@withContext null

            val longEdge = maxOf(sampled.width, sampled.height)
            if (longEdge <= maxDimen) return@withContext sampled

            val scaleFactor = maxDimen.toFloat() / longEdge
            val targetW = (sampled.width * scaleFactor).toInt().coerceAtLeast(1)
            val targetH = (sampled.height * scaleFactor).toInt().coerceAtLeast(1)
            val scaled = sampled.scale(targetW, targetH, filter = true)
            if (scaled !== sampled) sampled.recycle()
            scaled
        }

    suspend fun decodeScaledBitmap(
        context: Context,
        uri: android.net.Uri,
        maxDimen: Int = MAX_PAGE_DIMEN
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("decode_src_", ".tmp", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@withContext null
                decodeScaledBitmap(tempFile, maxDimen)
            } finally {
                tempFile.delete()
            }
        }

    suspend fun writeJpeg(
        bitmap: Bitmap,
        dir: File,
        quality: Int = 90,
        prefix: String = "opt_"
    ): File =
        withContext(Dispatchers.IO) {
            val outFile = File.createTempFile(prefix, ".jpg", dir)
            outFile.outputStream()
                .use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out) }
            bitmap.recycle()
            outFile
        }

    suspend fun autoEnhance(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val (low, high) = estimateLevels(bitmap)
        if (high - low < 20) return@withContext bitmap // not enough signal to safely stretch

        val scale = 255f / (high - low)
        val translate = -low * scale

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        bitmap.recycle()
        output
    }

    private fun estimateLevels(bitmap: Bitmap): Pair<Int, Int> {
        val sampleDim = 150
        val sw = minOf(sampleDim, bitmap.width)
        val sh = minOf(sampleDim, bitmap.height)
        val thumb = bitmap.scale(sw, sh)

        val pixels = IntArray(sw * sh)
        thumb.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        if (thumb !== bitmap) thumb.recycle()

        val histogram = IntArray(256)
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val luminance = (r * 299 + g * 587 + b * 114) / 1000
            histogram[luminance.coerceIn(0, 255)]++
        }

        val total = pixels.size
        val lowTarget = (total * 0.02f).toInt().coerceAtLeast(1)
        val highTarget = (total * 0.98f).toInt()

        var cumulative = 0
        var low = 0
        for (i in 0..255) {
            cumulative += histogram[i]
            if (cumulative >= lowTarget) {
                low = i; break
            }
        }

        cumulative = 0
        var high = 255
        for (i in 0..255) {
            cumulative += histogram[i]
            if (cumulative >= highTarget) {
                high = i; break
            }
        }

        return low to high
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimen: Int): Int {
        var inSampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while ((halfWidth / inSampleSize) >= maxDimen && (halfHeight / inSampleSize) >= maxDimen) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}