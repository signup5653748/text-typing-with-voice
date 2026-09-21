package com.example.core.editor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StarredFolder

@Composable
fun StarredFolderCard(
    folder: StarredFolder,
    onOpenFolder: () -> Unit,
    onFastSave: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF25334C), RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenFolder),
        color = Color(0xFF0F1524)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A263D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(0xFFFFC700),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            folder.name,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8),
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (folder.pathDisplay.isNotBlank()) {
                        Text(
                            folder.pathDisplay,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onFastSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Save",
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Unstar folder",
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileOrFolderRow(
    entry: FileEntry,
    onFolderClick: () -> Unit,
    onFileClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .clickable {
                if (entry.isDirectory) onFolderClick() else onFileClick()
            },
        color = if (entry.isDirectory) Color(0xFF0F1524) else Color(0xFF131B2D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (entry.isDirectory) Color(0xFFFFC700) else Color(0xFF56D0DE),
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        entry.name,
                        color = Color(0xFFECEFF8),
                        fontWeight = if (entry.isDirectory) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.sizeString.isNotBlank() || entry.lastModifiedString.isNotBlank()) {
                        Text(
                            listOf(entry.sizeString, entry.lastModifiedString).filter { it.isNotBlank() }.joinToString(" • "),
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!entry.isDirectory) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Tap to overwrite",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
