package com.scanify.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scanify.app.presentation.FileScreen
import com.scanify.app.presentation.HomeScreen
import com.scanify.app.presentation.MainScreen
import com.scanify.app.presentation.SettingScreen
import com.scanify.app.presentation.TemplateScreen

@Composable
fun Navigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val selectedTab = when {
        currentDestination?.hasRoute<Routes.HomeScreen>() == true -> "Home"
        currentDestination?.hasRoute<Routes.FileScreen>() == true -> "Files"
        currentDestination?.hasRoute<Routes.TemplateScreen>() == true -> "Templates"
        currentDestination?.hasRoute<Routes.SettingScreen>() == true -> "Settings"
        else -> "Home"
    }

    val showBottomBar = currentDestination?.hasRoute<Routes.HomeScreen>() == true ||
            currentDestination?.hasRoute<Routes.FileScreen>() == true ||
            currentDestination?.hasRoute<Routes.TemplateScreen>() == true ||
            currentDestination?.hasRoute<Routes.SettingScreen>() == true

    MainScreen(
        navController = navController,
        selectedTab = selectedTab,
        showBottomBar = showBottomBar
    ) {
        NavHost(
            navController = navController,
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


}