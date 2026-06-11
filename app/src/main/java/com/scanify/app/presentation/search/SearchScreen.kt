package com.scanify.app.presentation.search

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.scanify.app.domain.model.Document
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.DeleteDocumentDialog
import com.scanify.app.presentation.components.NoFilesScreen
import com.scanify.app.presentation.components.RenameDocumentDialog
import com.scanify.app.presentation.components.filecomponents.cards.ListFileCard
import com.scanify.app.presentation.components.moreoptioncomponents.MoreOptionsBottomSheet
import com.scanify.app.presentation.util.OfficeFileOpener
import com.scanify.app.presentation.viewmodels.FileNavigationEvent
import com.scanify.app.presentation.viewmodels.FileViewModel
import com.scanify.app.presentation.viewmodels.SearchUiState
import com.scanify.app.presentation.viewmodels.SearchViewModel
import com.scanify.app.ui.theme.BrandGradient
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = hiltViewModel(),
    fileViewModel: FileViewModel = hiltViewModel()
) {
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by searchViewModel.searchQuery.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var selectedDocForOptions by remember { mutableStateOf<Document?>(null) }
    var docToRename by remember { mutableStateOf<Document?>(null) }
    var docToDelete by remember { mutableStateOf<Document?>(null) }
    var docToExportLegacy by remember { mutableStateOf<Document?>(null) }

    val onCardClick = remember(fileViewModel) {
        { doc: Document ->
            keyboardController?.hide()
            fileViewModel.onDocumentClick(doc)
        }
    }
    val onMoreOptionsClick = remember {
        { doc: Document ->
            keyboardController?.hide()
            selectedDocForOptions = doc
        }
    }

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

    LaunchedEffect(fileViewModel.navigationEvent) {
        fileViewModel.navigationEvent.collect { event ->
            when (event) {
                is FileNavigationEvent.NavigateToPreview -> {
                    navController.navigate(Routes.PreviewScreen(id = event.documentId))
                }

                is FileNavigationEvent.OpenExternalFile -> {
                    OfficeFileOpener.openFile(context, event.filePath, event.fileType)
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

                            withContext(kotlinx.coroutines.Dispatchers.IO) {
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

                is FileNavigationEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { newQuery -> searchViewModel.onQueryChanged(newQuery) },
                    onSearchCancel = {
                        searchViewModel.onQueryChanged("")
                        keyboardController?.hide()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Text(
                        text = "Search any Document...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is SearchUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = BrandGradient.first(),
                        strokeWidth = 3.dp
                    )
                }

                is SearchUiState.NoResults -> {
                    NoFilesScreen(text = "No results found for \"$searchQuery\".")
                }

                is SearchUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.documents,
                            key = { it.id }
                        ) { document ->
                            ListFileCard(
                                document = document,
                                onClick = onCardClick,
                                onMoreOptionsClick = onMoreOptionsClick
                            )
                        }
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
                        fileViewModel.shareDocument(document)
                        selectedDocForOptions = null
                    },
                    onSaveClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            fileViewModel.autoSaveToDocuments(document)
                            selectedDocForOptions = null
                        } else {
                            docToExportLegacy = document
                            val extension = document.fileType.lowercase()
                            val fileNameWithExtension = "${document.name}.$extension"

                            exportDocumentLauncher.launch(fileNameWithExtension)
                            selectedDocForOptions = null
                        }
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
                    }
                )
            }
        }
    }
}