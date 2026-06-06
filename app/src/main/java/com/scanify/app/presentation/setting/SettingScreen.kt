package com.scanify.app.presentation.setting

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.scanify.app.domain.model.ExportState
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
    val context = LocalContext.current

    val currentThemeMode by viewModel.currentThemeMode.collectAsStateWithLifecycle()
    val exportState by viewModel.exportUiState.collectAsStateWithLifecycle()
    val docCount by viewModel.documentCount.collectAsStateWithLifecycle()

    val hasFiles = docCount > 0
    val isProcessing = exportState is ExportState.Processing

    val backupFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { safeUri ->
            viewModel.triggerFullBackupExport(safeUri)
        }
    }

    LaunchedEffect(exportState) {
        when (exportState) {
            is ExportState.Success -> {
                val filePath = (exportState as ExportState.Success).destinationPath
                Toast.makeText(context, "Export complete! Saved to: $filePath", Toast.LENGTH_LONG)
                    .show()
                viewModel.resetExportState()
            }

            is ExportState.Error -> {
                val errorMsg =
                    (exportState as ExportState.Error).throwable.localizedMessage ?: "Unknown Error"
                Toast.makeText(context, "Export failed: $errorMsg", Toast.LENGTH_LONG).show()
                viewModel.resetExportState()
            }

            else -> {}
        }
    }

    if (isProcessing) {
        val processingState = exportState as? ExportState.Processing
        val currentProgress = processingState?.progress ?: 0f
        val currentFile = processingState?.currentFileName ?: ""
        val percentage = (currentProgress * 100).toInt()

        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Exporting Backup",
                        style = MaterialTheme.typography.titleMedium
                    )

                    LinearProgressIndicator(
                        progress = { currentProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "Packaging: $currentFile ($percentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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

        // Backup Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Backup")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Rounded.Downloading,
                    iconBgColor = if (hasFiles) Color(0xFF2196F3) else Color.Gray,
                    title = "Backup all files",
                    subtitle = if (hasFiles) "Bundle DB and document assets to Documents/Scanify" else "No files available to backup",
                    showDivider = false,
                    onClick = {
                        if (hasFiles) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                viewModel.triggerFullBackupExport()
                            } else {
                                val timestamp = java.time.LocalDateTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                backupFilePickerLauncher.launch("Scanify_Backup_$timestamp.zip")
                            }
                        } else {
                            Toast.makeText(context, "Nothing to backup", Toast.LENGTH_SHORT).show()
                        }
                    },
                    trailingContent = {
                        if (hasFiles) ChevronIcon()
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
                    trailingContent = {
                        Text(
                            text = "1.0.0",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
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
