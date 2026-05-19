package com.scanify.app.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.scanify.app.navigation.Routes

@Composable
fun NavBar(navController: NavHostController, key: String) {
    val navItems = listOf(
        NavItem("Home", Icons.Default.Home, Routes.HomeScreen),
        NavItem("Files", Icons.Default.Description, Routes.FileScreen),
        NavItem("Templates", Icons.Default.DashboardCustomize, Routes.TemplateScreen),
        NavItem("Settings", Icons.Default.Tune, Routes.SettingScreen)
    )

    NavigationBar() {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = key == item.title,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = "Home icon"
                    )
                },
                label = {
                    Text(text = item.title)
                },
                onClick = {
                    if (key != item.title) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

            )
        }
    }
}

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val route: Routes
)