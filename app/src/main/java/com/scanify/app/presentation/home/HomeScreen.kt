package com.scanify.app.presentation.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.components.LoadingIndicator
import com.scanify.app.presentation.components.NoFilesScreen
import com.scanify.app.presentation.components.filecomponents.cards.ListFileCard
import com.scanify.app.presentation.home.components.PdfTool
import com.scanify.app.presentation.home.components.PdfToolCard
import com.scanify.app.presentation.util.OfficeFileOpener
import com.scanify.app.presentation.util.rememberDocumentScanner
import com.scanify.app.presentation.viewmodels.FileNavigationEvent
import com.scanify.app.presentation.viewmodels.FileUiState
import com.scanify.app.presentation.viewmodels.FileViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: FileViewModel = hiltViewModel()
) {
    val pdfTools = listOf(
        PdfTool("Signature", Icons.Rounded.Draw, Color(0xFF2196F3)),
        PdfTool("Watermark", Icons.AutoMirrored.Rounded.BrandingWatermark, Color(0xFF9C27B0)),
        PdfTool("Compress PDF", Icons.Rounded.Compress, Color(0xFF4CAF50)),
        PdfTool("Merge Files", Icons.Rounded.FolderZip, Color(0xFFFF9800))
    )

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val launchScanner = rememberDocumentScanner { imageUris, pdfUri ->
        viewModel.handleScannedDocuments(imageUris, pdfUri)
    }

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
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pdfTools.forEach { tool ->
                    PdfToolCard(
                        tool, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        when (val state = uiState) {
            is FileUiState.Loading -> {
                item {
                    Spacer(modifier = Modifier.height(50.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) { LoadingIndicator() }
                }
            }

            is FileUiState.Empty -> {
                item {
                    Spacer(modifier = Modifier.height(150.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) { NoFilesScreen("Recent", onScanNowClick = launchScanner) }
                }
            }

            is FileUiState.Success -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recent Documents",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "See all",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        navController.navigate(Routes.FileScreen) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                val recentDocs = state.documents.take(10)

                items(recentDocs, key = { it.id }) { doc ->
                    val onCardClick =
                        remember(doc) { { viewModel.onDocumentClick(doc) } }
                    ListFileCard(
                        document = doc, onClick = onCardClick
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
