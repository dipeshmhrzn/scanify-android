package com.scanify.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.scanify.app.navigation.Routes
import com.scanify.app.presentation.components.ListFileCard
import com.scanify.app.presentation.components.NoFilesScreen
import com.scanify.app.presentation.home.components.PdfTool
import com.scanify.app.presentation.home.components.PdfToolCard
import com.scanify.app.ui.theme.ScanifyTheme
import com.scanify.app.ui.theme.ThemeMode

@Composable
fun HomeScreen(navController: NavHostController) {
    val pdfTools = listOf(
        PdfTool("Signature", Icons.Rounded.Draw, Color(0xFF2196F3)),
        PdfTool("Watermark", Icons.AutoMirrored.Rounded.BrandingWatermark, Color(0xFF9C27B0)),
        PdfTool("Compress PDF", Icons.Rounded.Compress, Color(0xFF4CAF50)),
        PdfTool("Merge Files", Icons.Rounded.FolderZip, Color(0xFFFF9800))
    )


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),

        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pdfTools.forEach { tool ->

                PdfToolCard(
                    tool, modifier = Modifier.weight(1f)
                )
            }
        }

        val isWorkSpaceEmpty = false

        if (!isWorkSpaceEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                navController.navigate(Routes.FileScreen) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(15) {
                        ListFileCard()
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NoFilesScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    ScanifyTheme(ThemeMode.DARK) {
        HomeScreen(rememberNavController())
    }
}