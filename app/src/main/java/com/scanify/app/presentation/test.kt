package com.scanify.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Centralized colors
private val BackgroundColor = Color(0xFF131313)
private val TextPrimary = Color(0xFFF2F2F2)

// Vibrant colors matching the Settings aesthetic
private val BrandGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2196F3), Color(0xFF9C27B0))
)
private val ColorWatermark = Color(0xFFFF9800) // Orange
private val ColorCompress = Color(0xFF4CAF50) // Green
private val ColorMerge = Color(0xFFF44336) // Red

@Composable
fun ToolsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ToolItem(
            icon = Icons.Rounded.Draw,
            label = "Signature",
            backgroundBrush = BrandGradient
        )

        ToolItem(
            icon = Icons.Rounded.BrandingWatermark,
            label = "Watermark",
            backgroundColor = ColorWatermark
        )

        ToolItem(
            icon = Icons.Rounded.Compress,
            label = "Compress PDF",
            backgroundColor = ColorCompress
        )

        ToolItem(
            icon = Icons.Rounded.FolderZip,
            label = "Merge Files",
            backgroundColor = ColorMerge
        )
    }
}

@Composable
fun ToolItem(
    icon: ImageVector,
    label: String,
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        // Colorful Modern Icon Background
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (backgroundBrush != null) {
                        Modifier.background(backgroundBrush)
                    } else {
                        Modifier.background(backgroundColor)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Label
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ToolsSectionPreview() {
    MaterialTheme {
        ToolsSection()
    }
}
