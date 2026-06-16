package com.scanify.app.presentation.file

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.scanify.app.R
import com.scanify.app.presentation.components.LoadingIndicator
import com.scanify.app.presentation.components.filecomponents.preview.DocumentPageList
import com.scanify.app.presentation.lens.CoordinateMappingUtils
import com.scanify.app.presentation.lens.LensTextElement
import com.scanify.app.presentation.lens.LensUiState
import com.scanify.app.presentation.lens.LensViewModel
import com.scanify.app.presentation.util.rememberDocumentScanner
import com.scanify.app.presentation.viewmodels.PreviewUiState
import com.scanify.app.presentation.viewmodels.PreviewViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    documentId: Long,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PreviewViewModel = hiltViewModel(),
    lensViewModel: LensViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lensState by lensViewModel.lensState.collectAsStateWithLifecycle()

    var isLensActiveMode by remember { mutableStateOf(false) }
    val documentListState = rememberLazyListState()
    var capturedActivePageIndex by remember { mutableIntStateOf(0) }

    val launchScanner = rememberDocumentScanner(onLoading = {}, onSuccess = { imageUris, pdfUri ->
        viewModel.appendScannedImages(imageUris)
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val screenTitle = when (val state = uiState) {
                        is PreviewUiState.Success -> state.document.name
                        else -> "Preview"
                    }

                    Text(
                        text = if (isLensActiveMode) "Scanify Lens" else screenTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isLensActiveMode) {
                                isLensActiveMode = false
                                lensViewModel.resetToIdleState()
                            } else navController.popBackStack()
                        },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    if (uiState is PreviewUiState.Success) {
                        IconButton(
                            onClick = {
                                isLensActiveMode = !isLensActiveMode
                                if (isLensActiveMode) {

                                    val layoutInfo = documentListState.layoutInfo
                                    val viewportCenter = layoutInfo.viewportEndOffset / 2
                                    val centerItem = layoutInfo.visibleItemsInfo.minByOrNull {
                                        kotlin.math.abs((it.offset + (it.size / 2)) - viewportCenter)
                                    }

                                    capturedActivePageIndex = centerItem?.index ?: 0

                                } else lensViewModel.resetToIdleState()
                            },
                            modifier = Modifier.size(46.dp)
                        ) {

                            Icon(
                                painter = painterResource(id = R.drawable.textrecognition),
                                contentDescription = "Extract Text",
                                tint = if (isLensActiveMode) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                modifier = Modifier.size(28.dp)

                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is PreviewUiState.Loading -> {
                    Box(
                        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }

                is PreviewUiState.Success -> {
                    if (!isLensActiveMode) {
                        DocumentPageList(
                            document = state.document,
                            pageCount = state.pageCount,
                            lastModified = state.lastModified,
                            onAppendPagesRequested = launchScanner,
                            lazyListState = documentListState
                        )
                        if (state.isUpdating) LinearProgressIndicator(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    } else {
                        // Implement Pager to allow scrolling through all pages in Lens Mode
                        val pagerState = rememberPagerState(
                            initialPage = capturedActivePageIndex, pageCount = { state.pageCount })

                        // Automatically run OCR whenever the user stops scrolling on a new page
                        LaunchedEffect(pagerState.settledPage) {
                            lensViewModel.analyzeDocumentSource(
                                context,
                                state.document.filePath,
                                state.document.fileType,
                                pagerState.settledPage
                            )
                        }

                        VerticalPager(
                            state = pagerState, modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val modelData: Any = if (state.document.fileType.uppercase() == "PDF") {
                                com.scanify.app.presentation.util.DocumentPageRequest(
                                    state.document.filePath, page, state.lastModified
                                )
                            } else java.io.File(state.document.filePath)

                            // Only render the interactive workspace on the actively focused page
                            if (page == pagerState.settledPage) {
                                when (val lens = lensState) {
                                    is LensUiState.Analyzing -> Box(
                                        Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }

                                    is LensUiState.Success -> {
                                        LensInteractiveWorkspace(
                                            imageModel = modelData,
                                            elements = lens.elements,
                                            intrinsicImageSize = lens.imageSize,
                                            onActionTriggered = { action, text ->
                                                if (action == "COPY") {
                                                    scope.launch {
                                                        val clipData = ClipData.newPlainText(
                                                            "Extracted Text", text
                                                        )
                                                        clipboard.setClipEntry(clipData.toClipEntry())
                                                    }
                                                    Toast.makeText(
                                                        context, "Copied", Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    context.startActivity(
                                                        Intent(
                                                            Intent.ACTION_VIEW,
                                                            "https://www.google.com/search?q=${
                                                                Uri.encode(
                                                                    text
                                                                )
                                                            }".toUri()
                                                        )
                                                    )
                                                }
                                            })
                                    }

                                    is LensUiState.Error -> Box(
                                        Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            lens.message, color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    else -> {}
                                }
                            } else {
                                AsyncImage(
                                    model = modelData,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                is PreviewUiState.Error -> {
                    Box(
                        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center

                    ) {
                        Text(
                            text = "An error occurred while loading the document preview workspace.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

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

    // Your Brand Gradient
    val brandGradient = remember {
        Brush.linearGradient(colors = listOf(Color(0xFF2196F3), Color(0xFF9C27B0)))
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
                    // Retrieve pre-computed path instead of building it on every frame
                    val path = precomputedPaths[item] ?: return@forEach
                    val isSelected = item == selectedElement

                    if (isSelected) {
                        drawPath(path, colorScheme.primary.copy(alpha = 0.25f))
                        drawPath(path, brandGradient, style = Stroke(1.5.dp.toPx()))
                    } else {
                        val idleHighlight = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f)
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

                // Convert DP to true Pixels to guarantee the popup never overlaps the text box
                val yOffsetPx = with(density) { 68.dp.toPx() }.toInt()
                // Center the popup horizontally relative to the selected text block
                val popupWidthPx = with(density) { 200.dp.toPx() }.toInt()

                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(
                        x = (rect.center.x.toInt() - (popupWidthPx / 2)).coerceAtLeast(16),
                        y = (rect.top.toInt() - yOffsetPx).coerceAtLeast(16)
                    ),
                    properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
                    onDismissRequest = { selectedElement = null }) {
                    // Fully themed popup using ScanifyTheme colors
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.surface,
                        contentColor = colorScheme.onSurface,
                        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.2f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.clickable {
                                    onActionTriggered(
                                        "COPY", item.text
                                    )
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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