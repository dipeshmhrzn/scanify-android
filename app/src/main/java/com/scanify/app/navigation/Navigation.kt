package com.scanify.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntOffset
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
fun Navigation(
    isCompleted: Boolean,
    onOnboardingFinished: () -> Unit
) {
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

    val navAnimationSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val navOffsetSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)

    MainScreen(
        navController = navController,
        selectedTab = selectedTab,
        showBottomBar = showBottomBar,
        showFAB = showFAB
    ) {
        NavHost(
            navController = navController,
            startDestination = if (isCompleted) Routes.HomeScreen else Routes.OnBoardingScreen,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = navOffsetSpec
                ) + fadeIn(animationSpec = navAnimationSpec)
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = navOffsetSpec
                ) + fadeOut(animationSpec = navAnimationSpec)
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = navOffsetSpec
                ) + fadeIn(animationSpec = navAnimationSpec)
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = navOffsetSpec
                ) + fadeOut(animationSpec = navAnimationSpec)
            }
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

            composable<Routes.PreviewScreen>{ backStackEntry ->
                val previewRoute: Routes.PreviewScreen = backStackEntry.toRoute()
                PreviewScreen(
                    documentId = previewRoute.id,
                    navController = navController
                )
            }

            composable<Routes.SearchScreen>{
                SearchScreen(navController = navController)
            }

            composable<Routes.OnBoardingScreen>{
                OnboardingScreen(
                    navController = navController,
                    onOnboardingFinished = onOnboardingFinished
                )
            }
        }
    }


}