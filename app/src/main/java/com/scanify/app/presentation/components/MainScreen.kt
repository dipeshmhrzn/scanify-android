package com.scanify.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MainScreen(
    navController: NavHostController,
    selectedTab: String,
    showBottomBar: Boolean,
    showFAB: Boolean,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { it }
                )
            ) {
                NavBar(navController, selectedTab)
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFAB,
                enter = scaleIn(animationSpec = tween(300)),
                exit = scaleOut(animationSpec = tween(300))
            ) {
                CustomFAB()
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp,
                    top = innerPadding.calculateTopPadding()
                )
        ) {
            content()
        }
    }
}