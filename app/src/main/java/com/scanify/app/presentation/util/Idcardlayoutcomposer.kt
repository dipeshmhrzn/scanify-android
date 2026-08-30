package com.scanify.app.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object IdCardLayoutComposer {

    private const val PRINT_DPI = 300f
    private const val MM_PER_INCH = 25.4f
    private fun mmToPx(mm: Float): Int = ((mm / MM_PER_INCH) * PRINT_DPI).toInt()

    private val PAGE_WIDTH_PX = mmToPx(210f)   // A4 width
    private val PAGE_HEIGHT_PX = mmToPx(297f)  // A4 height
    private val ID1_LONG_PX = mmToPx(85.60f)   // ISO/IEC 7810 ID-1 long side
    private val ID1_SHORT_PX = mmToPx(53.98f)  // ISO/IEC 7810 ID-1 short side
    private const val CARD_GAP_PX = 150

    suspend fun composeReadyToPrintPage(context: Context, frontFile: File, backFile: File?): File? =
        withContext(Dispatchers.IO) {
            val frontBitmap = ImageOptimizer.decodeScaledBitmap(frontFile) ?: return@withContext null
            val backBitmap = backFile?.let { ImageOptimizer.decodeScaledBitmap(it) }

            val page = createBitmap(PAGE_WIDTH_PX, PAGE_HEIGHT_PX)
            val canvas = Canvas(page)
            canvas.drawColor(Color.WHITE)

            val frontSize = cardBoxFor(frontBitmap)
            val backSize = backBitmap?.let { cardBoxFor(it) }

            if (backSize != null) {
                val totalContentHeight = frontSize.second + CARD_GAP_PX + backSize.second
                val topMargin = (PAGE_HEIGHT_PX - totalContentHeight) / 2f

                val frontX = (PAGE_WIDTH_PX - frontSize.first) / 2f
                val frontRect = RectF(frontX, topMargin, frontX + frontSize.first, topMargin + frontSize.second)

                val backX = (PAGE_WIDTH_PX - backSize.first) / 2f
                val backTop = frontRect.bottom + CARD_GAP_PX
                val backRect = RectF(backX, backTop, backX + backSize.first, backTop + backSize.second)

                drawCardImage(canvas, frontBitmap, frontRect)
                drawCardImage(canvas, backBitmap, backRect)
                backBitmap.recycle()
            } else {
                val frontX = (PAGE_WIDTH_PX - frontSize.first) / 2f
                val topMargin = (PAGE_HEIGHT_PX - frontSize.second) / 2f
                val frontRect = RectF(frontX, topMargin, frontX + frontSize.first, topMargin + frontSize.second)
                drawCardImage(canvas, frontBitmap, frontRect)
            }

            frontBitmap.recycle()

            val outFile = File.createTempFile("id_card_layout_", ".jpg", context.cacheDir)
            outFile.outputStream().use { out -> page.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            page.recycle()
            outFile
        }

    private fun cardBoxFor(bitmap: Bitmap): Pair<Int, Int> =
        if (bitmap.width >= bitmap.height) {
            ID1_LONG_PX to ID1_SHORT_PX   // landscape capture -> landscape box
        } else {
            ID1_SHORT_PX to ID1_LONG_PX   // portrait capture -> portrait box
        }

    private fun drawCardImage(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
        val srcRect = centerCropRect(bitmap.width, bitmap.height, destRect.width() / destRect.height())
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, destRect, imagePaint)
    }

    private fun centerCropRect(srcWidth: Int, srcHeight: Int, dstAspect: Float): Rect {
        val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
        return if (srcAspect > dstAspect) {
            val cropWidth = (srcHeight * dstAspect).toInt().coerceAtMost(srcWidth)
            val left = (srcWidth - cropWidth) / 2
            Rect(left, 0, left + cropWidth, srcHeight)
        } else {
            val cropHeight = (srcWidth / dstAspect).toInt().coerceAtMost(srcHeight)
            val top = (srcHeight - cropHeight) / 2
            Rect(0, top, srcWidth, top + cropHeight)
        }
    }
}