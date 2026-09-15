package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        viewModel.jumpStart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("TOP", color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        viewModel.jumpEnd()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("END", color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        viewModel.selectAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("SELECT ALL", color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        onDismiss()
                        viewModel.openVoiceReplacePopup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("REPLACE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            HorizontalDivider(color = Color(0xFF222B3F), thickness = 1.dp)

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
    val activeColor = Color(0xFF3B82F6)
    val inactiveBadgeBg = Color(0xFF1C2438)
    val activeBadgeBg = activeColor.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161E30))
            .border(1.dp, Color(0xFF222B3F), RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) activeBadgeBg else inactiveBadgeBg)
                .border(
                    1.dp,
                    if (isActive) activeColor else Color(0xFF2E3D5C),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeLetter,
                color = if (isActive) activeColor else Color(0xFF8FA7D8),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFECEFF8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF6B7FA8)
            )
        }

        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = Color(0xFF8FA7D8),
                uncheckedTrackColor = Color(0xFF1C2438)
            )
        )
    }
}
