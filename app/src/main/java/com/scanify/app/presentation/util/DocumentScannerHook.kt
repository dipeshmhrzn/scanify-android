package com.scanify.app.presentation.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@Composable
fun rememberDocumentScanner(
    onLoading: (Boolean) -> Unit,
    onSuccess: (imageUris: List<String>) -> Unit
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

    val optimizeSemaphore = remember { Semaphore(3) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.let { res ->
                val rawImageUris = res.pages?.map { it.imageUri.toString() } ?: emptyList()

                scope.launch {
                    onLoading(true)
                    try {
                        val optimizedImageUris = kotlinx.coroutines.coroutineScope {
                            rawImageUris.map { uriStr ->
                                async(Dispatchers.Default) {
                                    optimizeSemaphore.withPermit {
                                        optimizeScannedImage(context, uriStr)
                                    }
                                }
                            }.awaitAll().filterNotNull()
                        }

                        if (optimizedImageUris.isEmpty()) {
                            Toast.makeText(context, "Couldn't process the scanned pages.", Toast.LENGTH_LONG).show()
                        } else {
                            onSuccess(optimizedImageUris)
                        }
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
    return try {
        val decoded = ImageOptimizer.decodeScaledBitmap(context, uriString.toUri()) ?: return null
        val enhanced = ImageOptimizer.autoEnhance(decoded)
        val outFile = ImageOptimizer.writeJpeg(enhanced, context.cacheDir, quality = 90, prefix = "opt_scan_")
        Uri.fromFile(outFile).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}