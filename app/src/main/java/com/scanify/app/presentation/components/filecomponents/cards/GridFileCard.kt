package com.scanify.app.presentation.components.filecomponents.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.scanify.app.presentation.components.filecomponents.badge.getFileTypeColors
import com.scanify.app.presentation.components.filecomponents.preview.DocumentThumbnail
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun GridFileCard(
    document: Document,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val fileColors = getFileTypeColors(document.fileType)
    val topBgColor = fileColors.second

    val cardShape = remember { RoundedCornerShape(16.dp) }
    val topImageShape = remember { RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp) }

    val formattedDate = remember(document.createdAt) { formatDateString(document.createdAt) }
    val displayFileType = remember(document.isImageBundle, document.fileType) {
        if (document.isImageBundle) "IMG" else document.fileType
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(topImageShape)
                    .background(topBgColor.copy(alpha = 0.5f))
            ) {

                DocumentThumbnail(document = document, modifier = Modifier.fillMaxSize())

                FileTypeBadge(
                    fileType = displayFileType,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )

                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "File Options",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                        .clickable {
                            onOptionsClick()
                        })
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.fileSize,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

fun formatDateString(dateTime: LocalDateTime): String {
    val fileDate = dateTime.toLocalDate()
    val today = LocalDate.now()

    return when (fileDate) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> fileDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    }
}