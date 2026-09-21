package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.core.editor.dialogs.DraftActionOptions
import com.example.core.editor.dialogs.DraftSummaryCard
import com.example.presentation.editor.EditorViewModel

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
                DraftSummaryCard(
                    draft = currentDraft,
                    lineCount = lineCount,
                    wordCount = wordCount,
                    previewText = previewText
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Options
                DraftActionOptions(
                    onKeepEditing = { viewModel.keepEditingDraft() },
                    onOpenOriginal = if (currentDraft.uriString.isNotBlank()) {
                        { viewModel.openLastSavedFile() }
                    } else null,
                    onStartNew = { viewModel.startNewDocument() }
                )
            }
        }
    }
}
