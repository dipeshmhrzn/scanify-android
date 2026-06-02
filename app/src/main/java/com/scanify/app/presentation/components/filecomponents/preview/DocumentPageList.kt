package com.scanify.app.presentation.components.filecomponents.preview

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.scanify.app.domain.model.Document
import com.scanify.app.presentation.util.DocumentPageRequest
import java.io.File

@Composable
fun DocumentPageList(
    document: Document,
    pageCount: Int,
    lastModified: Long,
    onAppendPagesRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val numPointers = event.changes.size

                        if (scale > 1f || numPointers > 1) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                            scale = nextScale

                            if (nextScale > 1f) {
                                val maxOffsetX = (nextScale - 1f) * size.width / 2f
                                val maxOffsetY = (nextScale - 1f) * size.height / 2f

                                offset = Offset(
                                    x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = scale == 1f
        ) {
            items(
                count = pageCount,
                key = { index -> "${document.id}_page_node_$index" }
            ) { index ->

                val isPdf = document.fileType.uppercase() == "PDF"
                val imageModel = remember(document.filePath, index, lastModified) {
                    if (isPdf) {
                        DocumentPageRequest(document.filePath, index, lastModified)
                    } else {
                        ImageRequest.Builder(context)
                            .data(File(document.filePath))
                            .memoryCacheKey("${document.filePath}_$lastModified")
                            .diskCacheKey("${document.filePath}_$lastModified")
                            .build()
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.707f) // 1. MOVED HERE: Lock the Card's height permanently
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(4.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Document Sheet ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            if (document.isImageBundle) {
                item(key = "append_bundle_footer_action") {
                    Button(
                        onClick = onAppendPagesRequested,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Page",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
