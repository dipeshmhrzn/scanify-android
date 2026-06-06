package com.scanify.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.util.rememberDocumentScanner
import com.scanify.app.presentation.viewmodels.FileViewModel

@Composable
fun MainScreen(
    navController: NavHostController,
    selectedTab: String,
    showBottomBar: Boolean,
    showFAB: Boolean,
    viewModel: FileViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {

    var isOptimizing by remember { mutableStateOf(false) }

    val animationSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val intOffsetAnimationSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)

    val launchScanner = rememberDocumentScanner(
        onLoading = { loading -> isOptimizing = loading },
        onSuccess = { imageUris, pdfUri ->
            viewModel.handleScannedDocuments(imageUris, pdfUri)
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(animationSpec = intOffsetAnimationSpec, initialOffsetY = { it }),
                exit = slideOutVertically(animationSpec = intOffsetAnimationSpec, targetOffsetY = { it })
            ) {
                NavBar(navController, selectedTab)
            }
        },
        topBar = {
            AnimatedVisibility(
                visible = showFAB,
                enter = slideInVertically(animationSpec = intOffsetAnimationSpec, initialOffsetY = { -it }),
                exit = slideOutVertically(animationSpec = intOffsetAnimationSpec, targetOffsetY = { -it })
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(16.dp)
                ) {
                    CustomSearchBar(onClick = {
                        navController.navigate(Routes.SearchScreen)
                    })
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFAB,
                enter = scaleIn(animationSpec = tween(300)),
                exit = scaleOut(animationSpec = tween(300))
            ) {
                CustomFAB(launchScanner)
            }
        }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
        if (isOptimizing) {
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
    }
}

