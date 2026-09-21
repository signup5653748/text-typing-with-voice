package com.example.core.editor.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SessionDraft

@Composable
fun DraftSummaryCard(
    draft: SessionDraft,
    lineCount: Int,
    wordCount: Int,
    previewText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = Color(0xFF0C1220),
        border = BorderStroke(1.dp, Color(0xFF1E2A3E))
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
                        draft.fileName,
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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF141D2F),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (previewText.length < draft.text.length) "\"$previewText...\"" else "\"$previewText\"",
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
}

@Composable
fun DraftActionOptions(
    onKeepEditing: () -> Unit,
    onOpenOriginal: (() -> Unit)?,
    onStartNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onKeepEditing,
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

        if (onOpenOriginal != null) {
            OutlinedButton(
                onClick = onOpenOriginal,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF101726),
                    contentColor = Color(0xFFECEFF8)
                ),
                border = BorderStroke(1.dp, Color(0xFF26354D)),
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

        OutlinedButton(
            onClick = onStartNew,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF94A3B8)
            ),
            border = BorderStroke(1.dp, Color(0xFF243048)),
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
