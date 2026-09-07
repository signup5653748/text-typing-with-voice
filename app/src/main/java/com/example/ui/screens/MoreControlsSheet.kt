package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
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
        containerColor = Color(0xFF141926),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF333E56)
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

            HorizontalDivider(color = Color(0xFF222B3F), thickness = 1.dp)

            // Quick Jump Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.jumpStart()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2438)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(
                        Icons.Default.VerticalAlignTop,
                        contentDescription = "Jump Top",
                        tint = Color(0xFF8FA7D8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Top",
                        color = Color(0xFFECEFF8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        viewModel.jumpEnd()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2438)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(
                        Icons.Default.VerticalAlignBottom,
                        contentDescription = "Jump Bottom",
                        tint = Color(0xFF8FA7D8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bottom",
                        color = Color(0xFFECEFF8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Voice Language Picker & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        viewModel.openLanguagePicker()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2438)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Language",
                        tint = Color(0xFF8FA7D8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = settings.voiceLanguage,
                        color = Color(0xFFECEFF8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2438)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF8FA7D8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        color = Color(0xFFECEFF8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlToggleRow(
    badgeLetter: String,
    title: String,
    description: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    val activeBg = Color(0xFF2563EB)
    val activeBorder = Color(0xFF93C5FD)
    val inactiveBg = Color(0xFF1E2638)
    val inactiveBorder = Color(0xFF2E3850)

    val badgeBg = if (isActive) activeBg else inactiveBg
    val badgeBorder = if (isActive) activeBorder else inactiveBorder

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF182032),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .border(1.dp, badgeBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeLetter,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (badgeLetter.length > 1) 12.sp else 16.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFECEFF8),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = Color(0xFF8E9BB8),
                    fontSize = 11.5.sp
                )
            }

            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2563EB),
                    uncheckedThumbColor = Color(0xFF8E9BB8),
                    uncheckedTrackColor = Color(0xFF222B3F)
                )
            )
        }
    }
}
