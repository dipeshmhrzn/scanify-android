package com.scanify.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NoFilesScreen(
    text: String,
    onScanNowClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(shape = CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(shape = CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "No files found",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = text,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (onScanNowClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(shape = RoundedCornerShape(16.dp))
                    .clickable { onScanNowClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)

            ) {
                Text(
                    text = "Scan now",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}