package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SessionResumeDialog(
    viewModel: EditorViewModel
) {
    val draft by viewModel.savedDraft.collectAsState()
    val showDialog by viewModel.showResumePopup.collectAsState()

    if (!showDialog || draft == null || draft?.text.isNullOrBlank()) {
        return
    }

    val currentDraft = draft!!
    val wordCount = currentDraft.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val charCount = currentDraft.text.length
    val lineCount = currentDraft.text.lines().size
    val previewText = currentDraft.text.trim().take(120)

    BackHandler(enabled = true) {
        viewModel.keepEditingDraft()
    }

    Dialog(
        onDismissRequest = { viewModel.keepEditingDraft() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF2B3A54), RoundedCornerShape(20.dp)),
            color = Color(0xFF131A2A),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = null,
                                tint = Color(0xFF56D0DE),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "Previous Session Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFECEFF8)
                            )
                            Text(
                                "Would you like to resume your draft?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.keepEditingDraft() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Draft Summary Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFF0C1220),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    currentDraft.fileName,
                                    color = Color(0xFFECEFF8),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                "$lineCount ${if (lineCount == 1) "line" else "lines"} • $wordCount words",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preview Snippet
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF141D2F),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (previewText.length < currentDraft.text.length) "\"$previewText...\"" else "\"$previewText\"",
                                color = Color(0xFFB0BFD8),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Primary Action: Keep Editing
                    Button(
                        onClick = { viewModel.keepEditingDraft() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Keep Editing Draft",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // 2. Open Last Saved File (if URI exists)
                    if (currentDraft.uriString.isNotBlank()) {
                        OutlinedButton(
                            onClick = { viewModel.openLastSavedFile() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF101726),
                                contentColor = Color(0xFFECEFF8)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26354D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFFFFC700),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Open Original Saved File",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 3. Start New Document
                    OutlinedButton(
                        onClick = { viewModel.startNewDocument() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF94A3B8)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243048)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            Icons.Default.NoteAdd,
                            contentDescription = null,
                            tint = Color(0xFF56D0DE),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start New (Blank Document)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
