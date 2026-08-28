package com.scanify.app.presentation.setting

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.scanify.app.BuildConfig
import com.scanify.app.domain.model.ExportState
import com.scanify.app.presentation.setting.components.ChevronIcon
import com.scanify.app.presentation.setting.components.EmailBottomSheet
import com.scanify.app.presentation.setting.components.SectionTitle
import com.scanify.app.presentation.setting.components.SettingsCard
import com.scanify.app.presentation.setting.components.SettingsRow
import com.scanify.app.presentation.util.sendEmail
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

    var activeSheetType by remember { mutableStateOf<EmailSheetType?>(null) }

    val backupFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { safeUri ->
            viewModel.triggerFullBackupExport(safeUri)
        }
    }

    val openPrivacyPolicy = {
        val url = "https://sites.google.com/view/scanify-labs-privacy/home"
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        intent.launchUrl(context, url.toUri())
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {

        // APPEARANCE SECTION
        item {
            Spacer(modifier = Modifier.height(8.dp))
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
                val processingState = exportState as? ExportState.Processing
                val progressPercent = ((processingState?.progress ?: 0f) * 100).toInt()

                SettingsRow(
                    icon = Icons.Rounded.Downloading,
                    iconBgColor = if (hasFiles) Color(0xFF2196F3) else Color.Gray,
                    title = "Backup all files",
                    subtitle = when {
                        isProcessing -> "Backing up... $progressPercent%"
                        hasFiles -> "Bundle DB and document assets to Documents/Scanify"
                        else -> "No files available to backup"
                    },
                    showDivider = false,
                    onClick = {

                        if (isProcessing) return@SettingsRow

                        if (hasFiles) {
                            Toast.makeText(context, "Backup started.", Toast.LENGTH_SHORT).show()
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
                        if (isProcessing) {
                            CircularProgressIndicator(
                                progress = { processingState?.progress ?: 0f },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (hasFiles) {
                            ChevronIcon()
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
                    trailingContent = {
                        Text(
                            text = BuildConfig.VERSION_NAME,
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
                    onClick = { openPrivacyPolicy() },
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
                    onClick = { activeSheetType = EmailSheetType.BUG },
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
                    onClick = { activeSheetType = EmailSheetType.FEEDBACK },
                    trailingContent = {
                        ChevronIcon()
                    }
                )
            }
        }

        item {
            activeSheetType?.let { config ->
                EmailBottomSheet(
                    title = config.title,
                    subtitle = config.subtitle,
                    subjectPlaceholder = config.subjectPlaceholder,
                    bodyPlaceholder = config.bodyPlaceholder,
                    onDismiss = { activeSheetType = null },
                    onSubmit = { subject, description ->
                        sendEmail(context, "${config.emailPrefix} $subject", description)
                    }
                )
            }
        }
    }
}


enum class EmailSheetType(
    val title: String,
    val subtitle: String,
    val subjectPlaceholder: String,
    val bodyPlaceholder: String,
    val emailPrefix: String
) {
    BUG(
        title = "Report a bug",
        subtitle = "Let us know any specific issue you experienced",
        subjectPlaceholder = "Title",
        bodyPlaceholder = "Describe the bug experienced as detailed as you can",
        emailPrefix = "[Bug Report]"
    ),
    FEEDBACK(
        title = "Share Feedback",
        subtitle = "We'd love to hear your thoughts and suggestions",
        subjectPlaceholder = "Subject",
        bodyPlaceholder = "Tell us what you like or how we can improve",
        emailPrefix = "[Feedback]"
    )
}