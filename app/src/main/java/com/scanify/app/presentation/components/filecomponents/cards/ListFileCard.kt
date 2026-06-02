package com.scanify.app.presentation.components.filecomponents.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanify.app.domain.model.Document
import com.scanify.app.presentation.components.filecomponents.badge.FileTypeBadge
import com.scanify.app.presentation.components.filecomponents.preview.DocumentThumbnail

@Composable
fun ListFileCard(
    document: Document, onClick: () -> Unit, modifier: Modifier = Modifier
) {

    val cardShape = remember { RoundedCornerShape(16.dp) }
    val thumbShape = remember { RoundedCornerShape(12.dp) }
    val optionBgShape = remember { RoundedCornerShape(8.dp) }

    val formattedDate = remember(document.createdAt) { formatDateString(document.createdAt) }

    val displayFileType = remember(document.isImageBundle, document.fileType) {
        if (document.isImageBundle) "IMG" else document.fileType
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentThumbnail(
                document = document,
                modifier = Modifier.size(64.dp).clip(thumbShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileTypeBadge(fileType = displayFileType)

                    Text(
                        text = "${document.fileSize}  •  $formattedDate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = { /* Handle options drop menu actions here */ })
                    .padding(8.dp), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "File Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}