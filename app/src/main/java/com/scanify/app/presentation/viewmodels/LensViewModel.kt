package com.scanify.app.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Size
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scanify.app.presentation.lens.LensTextElement
import com.scanify.app.presentation.lens.LensUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LensViewModel @Inject constructor() : ViewModel() {

    private val _lensState = MutableStateFlow<LensUiState>(LensUiState.Idle)
    val lensState = _lensState.asStateFlow()

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private var analysisJob: Job? = null

    fun analyzeDocumentSource(
        context: Context,
        filePath: String,
        fileType: String,
        targetPageIndex: Int = 0
    ) {
        analysisJob?.cancel()

        analysisJob = viewModelScope.launch {
            _lensState.value = LensUiState.Analyzing
            try {
                if (fileType.uppercase() == "PDF") {
                    processPdfSource(filePath, targetPageIndex)
                } else {
                    processImageSource(context, filePath)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _lensState.value = LensUiState.Error(e.localizedMessage ?: "Analysis failed.")
            }
        }
    }

    private suspend fun processPdfSource(filePath: String, pageIndex: Int) =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists()) {
                _lensState.value = LensUiState.Error("Target document path missing.")
                return@withContext
            }

            var bitmap: Bitmap? = null

            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex >= renderer.pageCount) {
                            _lensState.value = LensUiState.Error("Invalid page bounds.")
                            return@use
                        }
                        renderer.openPage(pageIndex).use { page ->
                            val scaleFactor = 2.5f
                            val bitmapWidth = (page.width * scaleFactor).toInt()
                            val bitmapHeight = (page.height * scaleFactor).toInt()

                            bitmap = createBitmap(bitmapWidth, bitmapHeight)

                            bitmap.let {
                                val canvas = Canvas(it)
                                canvas.drawColor(Color.WHITE)
                            }

                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )

                            executeOcrPipeline(
                                InputImage.fromBitmap(bitmap, 0),
                                Size(bitmapWidth.toFloat(), bitmapHeight.toFloat())
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _lensState.value = LensUiState.Error(e.localizedMessage ?: "PDF processing failed.")
            } finally {
                bitmap?.recycle()
            }
        }

    private suspend fun processImageSource(context: Context, filePath: String) =
        withContext(Dispatchers.IO) {
            val uri = if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
                filePath.toUri()
            } else {
                Uri.fromFile(File(filePath))
            }

            val intrinsicSize = context.contentResolver.openInputStream(uri).use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)
                Size(options.outWidth.toFloat(), options.outHeight.toFloat())
            }
            executeOcrPipeline(InputImage.fromFilePath(context, uri), intrinsicSize)
        }

    private suspend fun executeOcrPipeline(image: InputImage, dimensionSize: Size) {
        val visionText = recognizer.process(image).await()

        val elements = visionText.textBlocks.mapNotNull { block ->
            val box = block.boundingBox
            val points = block.cornerPoints
            if (box != null && points != null && points.size == 4) {
                LensTextElement(
                    id = UUID.randomUUID().toString(),
                    text = block.text,
                    rawBoundingBox = box,
                    cornerPoints = points.toList()
                )
            } else null
        }
        _lensState.value = LensUiState.Success(elements, dimensionSize)
    }

    fun resetToIdleState() {
        analysisJob?.cancel()
        _lensState.value = LensUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}