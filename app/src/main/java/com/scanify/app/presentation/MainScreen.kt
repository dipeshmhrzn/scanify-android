package com.scanify.app.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun MainScreen(
    navController: NavHostController,
    selectedTab: String,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = { NavBar(navController, selectedTab) }
    ) { innerPadding ->
        content(innerPadding)
    }
}