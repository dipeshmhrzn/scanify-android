package com.scanify.app.presentation.file

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.scanify.app.R
import com.scanify.app.domain.model.Document
import com.scanify.app.presentation.DeleteDocumentDialog
import com.scanify.app.presentation.components.LoadingIndicator
import com.scanify.app.presentation.components.RenameDocumentDialog
import com.scanify.app.presentation.components.filecomponents.preview.DocumentPageList
import com.scanify.app.presentation.file.components.PreviewBottomBar
import com.scanify.app.presentation.lens.LensUiState
import com.scanify.app.presentation.viewmodels.LensViewModel
import com.scanify.app.presentation.lens.LensInteractiveWorkspace
import com.scanify.app.presentation.util.rememberDocumentScanner
import com.scanify.app.presentation.viewmodels.FileNavigationEvent
import com.scanify.app.presentation.viewmodels.FileViewModel
import com.scanify.app.presentation.viewmodels.PreviewUiState
import com.scanify.app.presentation.viewmodels.PreviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    documentId: Long,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PreviewViewModel = hiltViewModel(),
    fileViewModel: FileViewModel = hiltViewModel(),
    lensViewModel: LensViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lensState by lensViewModel.lensState.collectAsStateWithLifecycle()
    val isSaving by fileViewModel.isSaving.collectAsStateWithLifecycle()

    var docToRename by remember { mutableStateOf<Document?>(null) }
    var docToDelete by remember { mutableStateOf<Document?>(null) }
    var docToExportLegacy by remember { mutableStateOf<Document?>(null) }

    var isLensActiveMode by remember { mutableStateOf(false) }
    val documentListState = rememberLazyListState()
    var capturedActivePageIndex by remember { mutableIntStateOf(0) }

    val launchScanner = rememberDocumentScanner(onLoading = {}, onSuccess = { imageUris, pdfUri ->
        viewModel.appendScannedImages(imageUris)
    })

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let { safeUri ->
            docToExportLegacy?.let { doc ->
                fileViewModel.saveToSelectedUri(doc, safeUri)
            }
        }
        docToExportLegacy = null
    }

    val currentDocument = (uiState as? PreviewUiState.Success)?.document

    LaunchedEffect(Unit) {
        fileViewModel.navigationEvent.collect { event ->
            when (event) {

                is FileNavigationEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }

                is FileNavigationEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is FileNavigationEvent.ShareFile -> {
                    try {
                        val originalFile = File(event.filePath)
                        if (originalFile.exists()) {
                            val extension = originalFile.extension
                            val cleanDisplayName = event.displayName.substringBeforeLast(".")
                            val finalizedShareName =
                                if (extension.isNotEmpty()) "$cleanDisplayName.$extension" else cleanDisplayName

                            val cacheDir =
                                File(context.cacheDir, "shared_documents").apply { mkdirs() }
                            val cacheFile = File(cacheDir, finalizedShareName)

                            withContext(Dispatchers.IO) {
                                cacheDir.listFiles()?.forEach { it.delete() }
                                originalFile.copyTo(cacheFile, overwrite = true)
                            }

                            val fileUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                cacheFile
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type =
                                    if (event.fileType.lowercase() == "pdf") "application/pdf" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                putExtra(Intent.EXTRA_TITLE, finalizedShareName)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share Document"
                                )
                            )
                        } else {
                            Toast.makeText(
                                context,
                                "Error: Physical file not found.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                else -> {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isLensActiveMode) "Scanify Lens" else (currentDocument?.name ?: "Preview"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(16.dp))
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
            bottomBar = {
                if (!isLensActiveMode) {
                    PreviewBottomBar(
                        onSaveClick = {
                            currentDocument?.let { document ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    fileViewModel.autoSaveToDocuments(document)
                                } else {
                                    docToExportLegacy = document
                                    val extension = document.fileType.lowercase()
                                    val fileNameWithExtension = "${document.name}.$extension"
                                    exportDocumentLauncher.launch(fileNameWithExtension)
                                }
                            }
                        },
                        onRenameClick = {
                            currentDocument?.let { docToRename = it }
                        },
                        onShareClick = {
                            currentDocument?.let { fileViewModel.shareDocument(it) }
                        },
                        onDeleteClick = {
                            currentDocument?.let { docToDelete = it }
                        }
                    )
                }
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            val pagerState = rememberPagerState(
                                initialPage = capturedActivePageIndex,
                                pageCount = { state.pageCount })

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
                                val modelData: Any =
                                    if (state.document.fileType.uppercase() == "PDF") {
                                        com.scanify.app.presentation.util.DocumentPageRequest(
                                            state.document.filePath, page, state.lastModified
                                        )
                                    } else java.io.File(state.document.filePath)

                                if (page == pagerState.settledPage) {
                                    when (val lens = lensState) {
                                        is LensUiState.Analyzing -> Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) { LoadingIndicator() }

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
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                lens.message,
                                                color = MaterialTheme.colorScheme.error
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
        if (isSaving) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF131324).copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }

        docToRename?.let { document ->
            RenameDocumentDialog(
                initialName = document.name,
                onDismiss = { docToRename = null },
                onConfirm = { newName ->
                    fileViewModel.renameDocument(document, newName)
                    docToRename = null
                }
            )
        }

        docToDelete?.let { document ->
            DeleteDocumentDialog(
                onDismiss = { docToDelete = null },
                onConfirm = {
                    fileViewModel.deleteDocument(document)
                    docToDelete = null
                    // Immediately exit the preview screen since the document no longer exists
                    navController.popBackStack()
                }
            )
        }
    }
}