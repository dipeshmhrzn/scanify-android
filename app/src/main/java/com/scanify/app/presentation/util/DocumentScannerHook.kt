package com.scanify.app.presentation.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun rememberDocumentScanner(
    onLoading: (Boolean) -> Unit,
    onSuccess: (imageUris: List<String>, pdfUri: String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    val scannerClient = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.let { res ->
                val rawImageUris = res.pages?.map { it.imageUri.toString() } ?: emptyList()

                scope.launch {
                    try {
                        onLoading(true)
                        val optimizedImageUris = rawImageUris.mapNotNull { uriStr ->
                            optimizeScannedImage(context, uriStr)
                        }

                        val optimizedPdfUri = if (optimizedImageUris.isNotEmpty()) {
                            createOptimizedPdf(context, optimizedImageUris)
                        } else null

                        onSuccess(optimizedImageUris, optimizedPdfUri)
                    } finally {
                        onLoading(false)
                    }
                }
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    return {
        val activity = context.findActivity()
        if (activity != null) {
            scannerClient.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG)
                        .show()
                }
        } else {
            Toast.makeText(context, "Context error: Unable to resolve Activity", Toast.LENGTH_SHORT)
                .show()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

private suspend fun optimizeScannedImage(context: Context, uriString: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val uri = uriString.toUri()

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val TARGET_MAX_DIMEN = 1000
            options.inSampleSize =
                calculateInSampleSize(options.outWidth, options.outHeight, TARGET_MAX_DIMEN)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val decodedBitmap = context.contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            val scale =
                (TARGET_MAX_DIMEN.toFloat() / decodedBitmap.width).coerceAtMost(TARGET_MAX_DIMEN.toFloat() / decodedBitmap.height)

            val finalBitmap = if (scale < 1.0f) {
                val targetW = (decodedBitmap.width * scale).toInt()
                val targetH = (decodedBitmap.height * scale).toInt()
                decodedBitmap.scale(targetW, targetH).also {
                    if (it != decodedBitmap) decodedBitmap.recycle()
                }
            } else {
                decodedBitmap
            }

            val optimizedFile = File.createTempFile("opt_scan_", ".jpg", context.cacheDir)
            FileOutputStream(optimizedFile).use { outStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 72, outStream)
            }

            finalBitmap.recycle()
            Uri.fromFile(optimizedFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private suspend fun createOptimizedPdf(
    context: Context,
    compressedImageUris: List<String>
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()

            val A4_WIDTH = 595
            val A4_HEIGHT = 842

            compressedImageUris.forEachIndexed { index, uriStr ->
                val file = File(uriStr.toUri().path ?: return@forEachIndexed)

                val options =
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
                val originalBitmap =
                    BitmapFactory.decodeFile(file.absolutePath, options) ?: return@forEachIndexed

                val isPortrait = originalBitmap.height >= originalBitmap.width
                val targetPageWidth = if (isPortrait) A4_WIDTH else A4_HEIGHT
                val targetPageHeight = if (isPortrait) A4_HEIGHT else A4_WIDTH

                val scaleX = targetPageWidth.toFloat() / originalBitmap.width
                val scaleY = targetPageHeight.toFloat() / originalBitmap.height
                val scale = scaleX.coerceAtMost(scaleY)

                val scaledWidth = (originalBitmap.width * scale).toInt()
                val scaledHeight = (originalBitmap.height * scale).toInt()

                val finalScaledBitmap = originalBitmap.scale(scaledWidth, scaledHeight)
                if (finalScaledBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }

                val pageInfo =
                    PdfDocument.PageInfo.Builder(targetPageWidth, targetPageHeight, index + 1)
                        .create()
                val page = pdfDocument.startPage(pageInfo)

                val paddingLeft = (targetPageWidth - scaledWidth) / 2f
                val paddingTop = (targetPageHeight - scaledHeight) / 2f

                page.canvas.drawBitmap(finalScaledBitmap, paddingLeft, paddingTop, null)
                pdfDocument.finishPage(page)

                finalScaledBitmap.recycle()
            }

            val pdfFile = File.createTempFile("opt_doc_", ".pdf", context.cacheDir)
            FileOutputStream(pdfFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }
            pdfDocument.close()

            Uri.fromFile(pdfFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxDimen: Int): Int {
    var inSampleSize = 1
    if (width > maxDimen || height > maxDimen) {
        val halfWidth = width / 2
        val halfHeight = height / 2
        while ((halfWidth / inSampleSize) >= maxDimen && (halfHeight / inSampleSize) >= maxDimen) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}