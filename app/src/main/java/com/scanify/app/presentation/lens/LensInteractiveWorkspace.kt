package com.scanify.app.presentation.lens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.scanify.app.ui.theme.BrandGradient

@Composable
fun LensInteractiveWorkspace(
    imageModel: Any,
    elements: List<LensTextElement>,
    intrinsicImageSize: Size,
    onActionTriggered: (String, String) -> Unit
) {
    var viewSize by remember { mutableStateOf(Size.Zero) }
    var selectedElement by remember { mutableStateOf<LensTextElement?>(null) }

    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme

    val mapper = remember(viewSize, intrinsicImageSize) {
        CoordinateMappingUtils.calculateMappingMatrix(viewSize, intrinsicImageSize)
    }

    val precomputedPaths = remember(elements, mapper) {
        elements.associateWith { mapper.buildPerspectivePath(it.cornerPoints) }
    }

    val brandGradient = remember {
        Brush.linearGradient(BrandGradient)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { viewSize = it.size.toSize() }
            .pointerInput(elements, mapper) {
                detectTapGestures { offset ->
                    selectedElement = elements.firstOrNull {
                        mapper.mapRect(it.rawBoundingBox).contains(offset)
                    }
                }
            }) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(Modifier.fillMaxSize()) {
            if (viewSize.width > 0f) {
                val scrimAlpha = if (isDark) 0.6f else 0.2f
                drawRect(Color.Black.copy(alpha = scrimAlpha))

                elements.forEach { item ->
                    val path = precomputedPaths[item] ?: return@forEach
                    val isSelected = item == selectedElement

                    if (isSelected) {
                        drawPath(path, colorScheme.primary.copy(alpha = 0.25f))
                        drawPath(path, brandGradient, style = Stroke(1.5.dp.toPx()))
                    } else {
                        val idleHighlight =
                            if (isDark) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f)
                        drawPath(path, idleHighlight)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedElement != null,
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + slideOutVertically { it / 4 }) {
            selectedElement?.let { item ->
                val rect = mapper.mapRect(item.rawBoundingBox)

                val yOffsetPx = with(density) { 68.dp.toPx() }.toInt()

                val popupWidthPx = with(density) { 200.dp.toPx() }.toInt()

                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(
                        x = (rect.center.x.toInt() - (popupWidthPx / 2)).coerceAtLeast(16),
                        y = (rect.top.toInt() - yOffsetPx).coerceAtLeast(16)
                    ),
                    properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
                    onDismissRequest = { selectedElement = null }) {

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.surface,
                        contentColor = colorScheme.onSurface,
                        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.clickable {
                                    onActionTriggered(
                                        "COPY", item.text
                                    )
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, "Copy", Modifier.size(18.dp))
                                Text("Copy", style = MaterialTheme.typography.labelLarge)
                            }

                            Row(
                                modifier = Modifier.clickable {
                                    onActionTriggered(
                                        "SEARCH", item.text
                                    )
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Search, "Search", Modifier.size(18.dp))
                                Text("Search", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}