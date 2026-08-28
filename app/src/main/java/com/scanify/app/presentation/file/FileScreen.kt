package com.scanify.app.presentation.file

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.scanify.app.domain.model.Document
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.DeleteDocumentDialog
import com.scanify.app.presentation.components.LoadingIndicator
import com.scanify.app.presentation.components.NoFilesScreen
import com.scanify.app.presentation.components.RenameDocumentDialog
import com.scanify.app.presentation.components.filecomponents.cards.GridFileCard
import com.scanify.app.presentation.components.filecomponents.cards.ListFileCard
import com.scanify.app.presentation.components.moreoptioncomponents.MoreOptionsBottomSheet
import com.scanify.app.presentation.file.components.QuickImportActionCard
import com.scanify.app.presentation.util.OfficeFileOpener
import com.scanify.app.presentation.util.rememberCappedLoadingState
import com.scanify.app.presentation.util.rememberDocumentScanner
import com.scanify.app.presentation.util.rememberSaveDocumentHandler
import com.scanify.app.presentation.viewmodels.FileNavigationEvent
import com.scanify.app.presentation.viewmodels.FileUiState
import com.scanify.app.presentation.viewmodels.FileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileScreen(
    navController: NavHostController, viewModel: FileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    var isGridView by remember { mutableStateOf(true) }
    val fileCategories = remember { listOf("All", "PDF", "DOCX", "PPTX", "XLSX") }

    var selectedDocForOptions by remember { mutableStateOf<Document?>(null) }
    var docToRename by remember { mutableStateOf<Document?>(null) }
    var docToDelete by remember { mutableStateOf<Document?>(null) }

    val onCardClick = remember(viewModel) {
        { doc: Document -> viewModel.onDocumentClick(doc) }
    }
    val onMoreOptionsClick = remember {
        { doc: Document -> selectedDocForOptions = doc }
    }

    val filteredDocs = remember(uiState, selectedCategory) {
        val state = uiState
        if (state is FileUiState.Success) {
            if (selectedCategory == "All") state.documents
            else state.documents.filter { it.fileType.equals(selectedCategory, ignoreCase = true) }
        } else {
            emptyList()
        }
    }

    val chunkedDocs = remember(filteredDocs) {
        filteredDocs.chunked(2)
    }

    val saveDocument = rememberSaveDocumentHandler(viewModel)

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is FileNavigationEvent.NavigateToPreview -> {
                    navController.navigate(Routes.PreviewScreen(id = event.documentId))
                }

                is FileNavigationEvent.OpenExternalFile -> {
                    OfficeFileOpener.openFile(context, event.filePath, event.fileType)
                }

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
            }
        }
    }

    var isOptimizing by remember { mutableStateOf(false) }

    val launchScanner = rememberDocumentScanner(
        onLoading = { loading -> isOptimizing = loading },
        onSuccess = { imageUris ->
            viewModel.handleScannedDocuments(imageUris)
        }
    )

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleFiles(uris.map { it.toString() })
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleImages(uris.map { it.toString() })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(contentType = "import_cards") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickImportActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Import Files",
                        icon = Icons.Default.UploadFile,
                        iconBgColor = Color(0xFF2196F3),
                        onClick = {

                            if (!isImporting) {
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.ms-powerpoint",
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                        "text/plain",
                                    )
                                )
                            }
                        })

                    QuickImportActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Import Images",
                        icon = Icons.Default.Image,
                        iconBgColor = Color(0xFF9C27B0),
                        onClick = {
                            if (!isImporting) {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        })
                }
            }

            when (val state = uiState) {
                is FileUiState.Loading -> {
                    item(contentType = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(0.8f)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) { LoadingIndicator() }
                    }
                }

                is FileUiState.Empty -> {
                    item(contentType = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(0.7f)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NoFilesScreen(
                                text = "No recent files found.",
                                onScanNowClick = launchScanner
                            )
                        }
                    }
                }

                is FileUiState.Success -> {

                    stickyHeader(contentType = "sticky_header") {
                        CategoryHeader(
                            fileCategories = fileCategories,
                            selectedCategory = selectedCategory,
                            isGridView = isGridView,
                            onCategorySelected = { selectedCategory = it },
                            onLayoutToggled = { isGridView = it }
                        )
                    }

                    if (filteredDocs.isEmpty()) {
                        item(contentType = "category_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NoFilesScreen(text = "No files found in category : \"$selectedCategory\"")
                            }
                        }
                    } else {
                        if (isGridView) {
                            items(
                                items = chunkedDocs,
                                key = { rowDocs -> rowDocs.joinToString("_") { it.id.toString() } },
                                contentType = { "grid_row" }
                            ) { rowDocs ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowDocs.forEach { doc ->
                                        GridFileCard(
                                            document = doc,
                                            onClick = onCardClick,
                                            onMoreOptionsClick = onMoreOptionsClick,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowDocs.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        } else {
                            items(
                                filteredDocs,
                                key = { it.id },
                                contentType = { "list_row" }) { doc ->
                                ListFileCard(
                                    document = doc,
                                    onClick = onCardClick,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onMoreOptionsClick = onMoreOptionsClick
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
        val showBlockingDialog = rememberCappedLoadingState(
            isActive = isOptimizing,
            onCapReached = {
                Toast.makeText(context, "Continuing in background...", Toast.LENGTH_SHORT).show()
            }
        )
        if (showBlockingDialog) {
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

        selectedDocForOptions?.let { document ->
            MoreOptionsBottomSheet(
                isVisible = true,
                onDismiss = { selectedDocForOptions = null },
                onRenameClick = {
                    docToRename = document
                    selectedDocForOptions = null
                },
                onShareClick = {
                    viewModel.shareDocument(document)
                    selectedDocForOptions = null
                },
                onSaveClick = {
                    saveDocument(document)
                    selectedDocForOptions = null
                },
                onDeleteClick = {
                    docToDelete = document
                    selectedDocForOptions = null
                }
            )
        }

        docToRename?.let { document ->
            RenameDocumentDialog(
                initialName = document.name,
                onDismiss = { docToRename = null },
                onConfirm = { newName ->
                    viewModel.renameDocument(document, newName)
                    docToRename = null
                }
            )
        }

        docToDelete?.let { document ->
            DeleteDocumentDialog(
                onDismiss = { docToDelete = null },
                onConfirm = {
                    viewModel.deleteDocument(document)
                    docToDelete = null
                }
            )
        }
    }
}


@Composable
private fun CategoryHeader(
    fileCategories: List<String>,
    selectedCategory: String,
    isGridView: Boolean,
    onCategorySelected: (String) -> Unit,
    onLayoutToggled: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(fileCategories, key = { it }) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(end = 16.dp, start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Switch to Grid Layout",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onLayoutToggled(true) },
                tint = if (isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = "Switch to List Layout",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onLayoutToggled(false) },
                tint = if (!isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}