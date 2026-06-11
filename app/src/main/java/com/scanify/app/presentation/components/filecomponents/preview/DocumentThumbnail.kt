package com.scanify.app.presentation.components.filecomponents.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import com.scanify.app.R
import com.scanify.app.domain.model.Document
import com.scanify.app.presentation.util.DocumentPageRequest
import java.io.File

@Composable
fun DocumentThumbnail(
    document: Document,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val density = context.resources.displayMetrics.density

    val targetSizePx = remember(density) { (240 * density).toInt() }

    val fileExtension = remember(document.fileType) { document.fileType.uppercase() }
    val isPdf = fileExtension == "PDF"
    val isImage = fileExtension in listOf("JPG", "JPEG", "PNG", "WEBP")

    val logoResource = remember(fileExtension) {
        when (fileExtension) {
            "DOC", "DOCX" -> R.drawable.ic_word_logo
            "XLS", "XLSX" -> R.drawable.ic_excel_logo
            "PPT", "PPTX" -> R.drawable.ic_ppt_logo
            else -> null
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (isPdf || isImage) {
            val imageModel = remember(document.filePath) {

                val file = File(document.filePath)
                val lastMod = if (file.exists()) file.lastModified() else 0L

                ImageRequest.Builder(context)
                    .data(
                        if (isPdf) {
                            DocumentPageRequest(
                                filePath = document.filePath,
                                pageIndex = 0,
                                lastModified = lastMod                          )
                        } else {
                            File(document.filePath)
                        }
                    )
                    .size(Size(targetSizePx, targetSizePx))
                    .precision(Precision.INEXACT)
                    .crossfade(true)
                    .build()
            }

            AsyncImage(
                model = imageModel,
                contentDescription = "${document.name} thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            if (logoResource != null) {
                Image(
                    painter = painterResource(id = logoResource),
                    contentDescription = "$fileExtension Logo",
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "Generic File Placeholder",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}