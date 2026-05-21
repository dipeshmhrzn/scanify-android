package com.scanify.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.scanify.app.R
import com.scanify.app.presentation.components.NoFilesScreen
import com.scanify.app.presentation.components.PdfTool
import com.scanify.app.presentation.components.PdfToolCard
import com.scanify.app.ui.theme.ScanifyTheme

@Composable
fun HomeScreen(navController: NavHostController) {
    val pdfTools = listOf(
        PdfTool("Signature", R.drawable.signature),
        PdfTool("Watermark", R.drawable.watermark),
        PdfTool("Compress PDF", R.drawable.compress),
        PdfTool("Merge Files", R.drawable.merge)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pdfTools.forEach { tool ->

                PdfToolCard(
                    tool, modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            NoFilesScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    ScanifyTheme() {
        HomeScreen(rememberNavController())
    }
}