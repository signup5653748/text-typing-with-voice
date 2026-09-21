package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.editor.dialogs.ControlToggleRow
import com.example.core.editor.dialogs.QuickNavigationActionsRow
import com.example.core.editor.dialogs.VoiceSettingsActionRow
import com.example.presentation.editor.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreControlsSheet(
    viewModel: EditorViewModel,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val kActive by viewModel.kActive.collectAsState()
    val pActive by viewModel.pActive.collectAsState()
    val kbLockActive by viewModel.kbLockActive.collectAsState()
    val settings by viewModel.settings.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131826),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF26324A))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Controls & Modes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFECEFF8)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8FA7D8)
                    )
                }
            }

            // Letter K Toggle Item
            ControlToggleRow(
                badgeLetter = "K",
                title = "Letter K (Character Mode)",
                description = if (kActive) "Active: Stepping character-by-character" else "Inactive: Stepping word-by-word",
                isActive = kActive,
                onToggle = viewModel::toggleK
            )

            // Letter P Toggle Item
            ControlToggleRow(
                badgeLetter = "P",
                title = "Letter P (Paragraph Mode)",
                description = if (pActive) "Active: Stepping paragraph-by-paragraph" else "Inactive: Stepping line-by-line",
                isActive = pActive,
                onToggle = viewModel::toggleP
            )

            // Keyboard Lock (KB Lock) Item
            ControlToggleRow(
                badgeLetter = "KB",
                title = "Keyboard Lock",
                description = if (kbLockActive) "Virtual keyboard is locked" else "Virtual keyboard opens on touch",
                isActive = kbLockActive,
                onToggle = viewModel::toggleKbLock
            )

            // Quick Edit Tools Row
            QuickNavigationActionsRow(
                onJumpStart = {
                    onDismiss()
                    viewModel.jumpStart()
                },
                onJumpEnd = {
                    onDismiss()
                    viewModel.jumpEnd()
                },
                onSelectAll = {
                    onDismiss()
                    viewModel.selectAll()
                },
                onReplace = {
                    onDismiss()
                    viewModel.openVoiceReplacePopup()
                }
            )

            HorizontalDivider(color = Color(0xFF222B3F), thickness = 1.dp)

            // Voice Language Picker & Settings
            VoiceSettingsActionRow(
                voiceLanguage = settings.voiceLanguage,
                onLanguagePickerClick = {
                    onDismiss()
                    viewModel.openLanguagePicker()
                },
                onSettingsClick = {
                    onDismiss()
                    onNavigateToSettings()
                }
            )
        }
    }
}
