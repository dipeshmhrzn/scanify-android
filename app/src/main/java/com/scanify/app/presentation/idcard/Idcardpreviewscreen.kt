package com.scanify.app.presentation.idcard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.components.LoadingIndicator
import com.scanify.app.presentation.util.rememberCappedLoadingState
import com.scanify.app.presentation.util.rememberDocumentScanner
import com.scanify.app.presentation.viewmodels.FileNavigationEvent
import com.scanify.app.presentation.viewmodels.IdCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCardPreviewScreen(
    frontUri: String,
    backUri: String?,
    navController: NavHostController,
    viewModel: IdCardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsState()
    var isRetaking by remember { mutableStateOf(false) }

    val retakeScanner = rememberDocumentScanner(
        onLoading = { loading -> isRetaking = loading },
        pageLimit = 2,
        onSuccess = { imageUris ->
            if (imageUris.isNotEmpty()) {
                navController.navigate(
                    Routes.IdCardPreviewScreen(
                        frontUri = imageUris[0],
                        backUri = imageUris.getOrNull(1)
                    )
                ) {
                    popUpTo(Routes.IdCardPreviewScreen(frontUri = frontUri, backUri = backUri)) {
                        inclusive = true
                    }
                }
            } else {
                Toast.makeText(context, "No page was captured.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is FileNavigationEvent.NavigateToPreview -> {
                    navController.navigate(Routes.PreviewScreen(id = event.documentId)) {
                        popUpTo(Routes.HomeScreen)
                    }
                }

                is FileNavigationEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }

                is FileNavigationEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                else -> Unit
            }
        }
    }

    val showBlockingDialog = rememberCappedLoadingState(
        isActive = isRetaking,
        onCapReached = {
            Toast.makeText(context, "Continuing in background...", Toast.LENGTH_SHORT).show()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm ID Card") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (backUri != null) {
                    "Review both sides before generating a print-ready PDF."
                } else {
                    "Review the card before generating a print-ready PDF."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IdCardPreviewCard(label = "FRONT", uri = frontUri)
            if (backUri != null) {
                IdCardPreviewCard(label = "BACK", uri = backUri)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { retakeScanner() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(if (backUri != null) "  Retake Both Sides" else "  Retake")
            }

            Button(
                onClick = { viewModel.generatePdf(frontUri, backUri) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {

                Text("Generate Print-Ready PDF")

            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF131324).copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    }

    if (showBlockingDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        )
    }
}

@Composable
private fun IdCardPreviewCard(label: String, uri: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        AsyncImage(
            model = uri,
            contentDescription = "$label side of ID card",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}
