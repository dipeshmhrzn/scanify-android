package com.scanify.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.scanify.app.presentation.components.MainScreen
import com.scanify.app.presentation.file.FileScreen
import com.scanify.app.presentation.file.PreviewScreen
import com.scanify.app.presentation.home.HomeScreen
import com.scanify.app.presentation.onboarding.OnboardingScreen
import com.scanify.app.presentation.search.SearchScreen
import com.scanify.app.presentation.setting.SettingScreen
import com.scanify.app.presentation.template.TemplateScreen

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

    val showFAB =
        currentDestination?.hasRoute<Routes.HomeScreen>() == true || currentDestination?.hasRoute<Routes.FileScreen>() == true



    MainScreen(
        navController = navController,
        selectedTab = selectedTab,
        showBottomBar = showBottomBar,
        showFAB = showFAB
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.OnBoardingScreen,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeOut(tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {

            composable<Routes.HomeScreen>(
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                HomeScreen(navController)
            }

            composable<Routes.FileScreen>(
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                FileScreen(navController)
            }

            composable<Routes.TemplateScreen>(
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }) {
                TemplateScreen(navController)
            }

            composable<Routes.SettingScreen>(
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }) {
                SettingScreen(navController)
            }

            composable<Routes.PreviewScreen>(
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }
            ) { backStackEntry ->
                val previewRoute: Routes.PreviewScreen = backStackEntry.toRoute()
                PreviewScreen(
                    documentId = previewRoute.id,
                    navController = navController
                )
            }

            composable<Routes.SearchScreen>(
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                SearchScreen(navController = navController)
            }

            composable<Routes.OnBoardingScreen>(
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                OnboardingScreen(navController = navController, onOnboardingFinished = {})
            }
        }
    }


}