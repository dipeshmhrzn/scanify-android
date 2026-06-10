package com.scanify.app.presentation.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.scanify.app.domain.model.InAppUpdateState
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.util.UpdateManager
import com.scanify.app.presentation.util.rememberDocumentScanner
import com.scanify.app.presentation.viewmodels.FileViewModel
import com.scanify.app.ui.theme.BrandGradient

@Composable
fun MainScreen(
    navController: NavHostController,
    selectedTab: String,
    showBottomBar: Boolean,
    showFAB: Boolean,
    updateManager: UpdateManager,
    viewModel: FileViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {

    val context = LocalContext.current
    var isOptimizing by remember { mutableStateOf(false) }

    val currentUpdateState by updateManager.updateState.collectAsStateWithLifecycle()

    val intOffsetAnimationSpec =
        tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)


    LaunchedEffect(currentUpdateState) {
        if (currentUpdateState is InAppUpdateState.Failed) {
            Toast.makeText(
                context,
                "Update failed. Retrying in background later.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

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
                enter = slideInVertically(
                    animationSpec = intOffsetAnimationSpec,
                    initialOffsetY = { it }),
                exit = slideOutVertically(
                    animationSpec = intOffsetAnimationSpec,
                    targetOffsetY = { it })
            ) {
                NavBar(navController, selectedTab)
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                AnimatedVisibility(
                    visible = showFAB,
                    enter = slideInVertically(
                        animationSpec = intOffsetAnimationSpec,
                        initialOffsetY = { -it }),
                    exit = slideOutVertically(
                        animationSpec = intOffsetAnimationSpec,
                        targetOffsetY = { -it })
                ) {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )
                    ) {
                        CustomSearchBar(onClick = { navController.navigate(Routes.SearchScreen) })
                    }
                }

                val topPaddingAdjustment = if (showFAB) 0.dp else 16.dp
                val brandGradient = Brush.horizontalGradient(colors = BrandGradient)

                AnimatedVisibility(
                    visible = currentUpdateState is InAppUpdateState.Downloading,
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
                ) {
                    val progressPercent =
                        (currentUpdateState as? InAppUpdateState.Downloading)?.progressPercent ?: 0
                    val progressFloat = progressPercent / 100f

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = topPaddingAdjustment,
                                bottom = 16.dp
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Downloading Scanify Update ($progressPercent%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.2f
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progressFloat)
                                        .fillMaxHeight()
                                        .background(
                                            brush = brandGradient,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = currentUpdateState is InAppUpdateState.Downloaded,
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = topPaddingAdjustment,
                                bottom = 16.dp
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update Ready",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Restart to optimize your application.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            Button(
                                onClick = { updateManager.completeUpdateInstallation() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "RESTART",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
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

        val animatedBottomPadding by animateDpAsState(
            targetValue = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            label = "paddingAnimation"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = animatedBottomPadding
                )
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

