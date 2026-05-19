package com.scanify.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scanify.app.presentation.FileScreen
import com.scanify.app.presentation.HomeScreen
import com.scanify.app.presentation.SettingScreen
import com.scanify.app.presentation.TemplateScreen

@Composable
fun Navigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController=navController,
        startDestination = Routes.HomeScreen
    ) {

        composable<Routes.HomeScreen> {
            HomeScreen(navController)
        }

        composable<Routes.FileScreen> {
            FileScreen(navController)
        }

        composable<Routes.TemplateScreen> {
            TemplateScreen(navController)
        }

        composable<Routes.SettingScreen> {
            SettingScreen(navController)
        }

    }

}