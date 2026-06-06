package com.scanify.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.scanify.app.navigation.Routes

@Composable
fun NavBar(navController: NavHostController, key: String) {
    val navItems = listOf(
        NavItem("Home", Icons.Default.Home, Routes.HomeScreen),
        NavItem("Files", Icons.Default.Description, Routes.FileScreen),
//        NavItem("Templates", Icons.Default.DashboardCustomize, Routes.TemplateScreen),
        NavItem("Settings", Icons.Default.Tune, Routes.SettingScreen)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = key == item.title,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = "${item.title} icon"
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
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