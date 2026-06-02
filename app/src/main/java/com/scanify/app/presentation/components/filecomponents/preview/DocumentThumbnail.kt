package com.scanify.app.presentation.components.filecomponents.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
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
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
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

    val file = File(document.filePath)
    val lastModified = if (file.exists()) file.lastModified() else 0L

    val imageModel = remember(key1 = document.filePath, key2 = lastModified) {
        val fileExtension = document.fileType.uppercase()
        val isPdf = fileExtension == "PDF"

        ImageRequest.Builder(context)
            .data(
                if (isPdf) {
                    DocumentPageRequest(document.filePath, pageIndex = 0, lastModified)
                } else {
                    file
                }
            )
            .memoryCacheKey("${document.filePath}_${lastModified}_thumbnail")
            .diskCacheKey("${document.filePath}_${lastModified}_thumbnail")
            .crossfade(true)
            .build()
    }
    SubcomposeAsyncImage(
        modifier = modifier,
        model = imageModel,
        contentDescription = "${document.name} thumbnail",
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val fileExtension = document.fileType.uppercase()

                val logoResource = when (fileExtension) {
                    "DOC", "DOCX" -> R.drawable.ic_word_logo
                    "XLS", "XLSX" -> R.drawable.ic_excel_logo
                    "PPT", "PPTX" -> R.drawable.ic_ppt_logo
                    else          -> null // Fallback indicator
                }

                if (logoResource != null) {
                    Image(
                        painter = painterResource(id = logoResource),
                        contentDescription = "$fileExtension Logo",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = "Generic File Placeholder",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}