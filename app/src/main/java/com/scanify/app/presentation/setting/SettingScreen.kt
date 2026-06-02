package com.scanify.app.presentation.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.scanify.app.presentation.setting.components.ChevronIcon
import com.scanify.app.presentation.setting.components.SectionTitle
import com.scanify.app.presentation.setting.components.SettingsCard
import com.scanify.app.presentation.setting.components.SettingsRow
import com.scanify.app.presentation.viewmodels.SettingViewModel
import com.scanify.app.ui.theme.BrandGradient


@Composable
fun SettingScreen(
    navController: NavHostController,
    viewModel: SettingViewModel = hiltViewModel()
) {

    val currentThemeMode by viewModel.currentThemeMode.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {

        // APPEARANCE SECTION
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("APPEARANCE")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Rounded.Brightness4,
                    iconBgColor = MaterialTheme.colorScheme.primary,
                    title = "Theme",
                    subtitle = "${
                        currentThemeMode.name.lowercase().replaceFirstChar { it.uppercase() }
                    } • Tap to change",
                    showDivider = false,
                    onClick = {
                        viewModel.cycleTheme()
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(colors = BrandGradient))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = currentThemeMode.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                )
            }
        }

        // ABOUT SECTION
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("ABOUT")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    iconBgColor = Color(0xFF9E9E9E),
                    title = "Version",
                    showDivider = true,
                    onClick = { },
                    trailingContent =
                        {
                            Text(
                                text = "1.0.1",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        })
                SettingsRow(
                    icon = Icons.Rounded.Description,
                    iconBgColor = MaterialTheme.colorScheme.primary,
                    title = "Privacy Policy",
                    showDivider = true,
                    onClick = { },
                    trailingContent = {
                        ChevronIcon()
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.BugReport,
                    iconBgColor = Color(0xFFF44336),
                    title = "Report a Bug",
                    subtitle = "Help us improve Scanify",
                    showDivider = true,
                    onClick = { },
                    trailingContent = {
                        ChevronIcon()
                    }
                )
                SettingsRow(
                    icon = Icons.AutoMirrored.Rounded.Chat,
                    iconBgColor = Color(0xFF4CAF50),
                    title = "Feedback",
                    subtitle = "Share your thoughts",
                    showDivider = false,
                    onClick = { },
                    trailingContent = {
                        ChevronIcon()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingScreenPreview() {
    MaterialTheme {
        SettingScreen(rememberNavController())
    }
}