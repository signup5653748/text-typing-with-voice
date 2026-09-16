package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel
) {
    val settings by viewModel.settings.collectAsState()

    var buttonOrder by remember(settings.buttonOrder) {
        val list = settings.buttonOrder.split(",").map { it.trim() }
            .filter { it.isNotBlank() && it != "MORE" && it != "REPLACE" && it != "REP" }
        if (list.size < 6) {
            mutableStateOf(listOf("CUT", "COPY", "DELETE", "PASTE", "SELECT_ALL", "ENTER", "TOP", "END", "K", "P", "KB_LOCK"))
        } else {
            mutableStateOf(list)
        }
    }

    val availableSlotActions = listOf(
        "CUT" to "Cut (CUT)",
        "COPY" to "Copy (COPY)",
        "DELETE" to "Delete (DEL)",
        "PASTE" to "Paste (PASTE)",
        "SELECT_ALL" to "Select All (ALL)",
        "ENTER" to "Enter Line (ENTER)",
        "TOP" to "Top of Document (TOP)",
        "END" to "End of Document (END)",
        "K" to "K Char/Word (K)",
        "P" to "P Para/Line (P)",
        "KB_LOCK" to "Keyboard Lock (KB)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Layout",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFECEFF8)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF8FA7D8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161A24)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0C0D10))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Button Size & Scaling Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Keypad Button Size Multiplier
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Action Button Size",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFECEFF8),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Surface(
                                    color = Color(0xFF20293D),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "${(settings.buttonSizeMultiplier * 100).toInt()}%",
                                        color = Color(0xFFA78BFA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Compact", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                                Slider(
                                    value = settings.buttonSizeMultiplier,
                                    onValueChange = { viewModel.updateButtonSizeMultiplier(it) },
                                    valueRange = 0.8f..1.5f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )
                                Text("Large", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                            }
                        }

                        HorizontalDivider(color = Color(0xFF243048))

                        // D-Pad Arrow Cluster Size Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "D-Pad Arrow Cluster Size",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFECEFF8),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Surface(
                                    color = Color(0xFF20293D),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "${(settings.arrowSize * 100).toInt()}%",
                                        color = Color(0xFFA78BFA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Small", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                                Slider(
                                    value = settings.arrowSize,
                                    onValueChange = { viewModel.updateArrowSize(it) },
                                    valueRange = 0.7f..1.6f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )
                                Text("Large", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                            }
                        }
                    }
                }
            }

            // Custom Button Arrangement Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Custom Button Arrangement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Use the up and down arrows to shift any action button into your preferred keypad slot position.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF8FA7D8)
                        )

                        buttonOrder.forEachIndexed { index, actionName ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF20293D),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF7C3AED)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            availableSlotActions.find { it.first == actionName }?.second ?: actionName,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val updated = buttonOrder.toMutableList()
                                                    val tmp = updated[index]
                                                    updated[index] = updated[index - 1]
                                                    updated[index - 1] = tmp
                                                    buttonOrder = updated
                                                    val enumList = updated.mapNotNull {
                                                        try { ActionButton.valueOf(if (it == "DEL") "DELETE" else it) } catch (_: Exception) { null }
                                                    }
                                                    viewModel.updateButtonOrder(enumList)
                                                }
                                            },
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) Color.White else Color(0xFF4A5568)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < buttonOrder.size - 1) {
                                                    val updated = buttonOrder.toMutableList()
                                                    val tmp = updated[index]
                                                    updated[index] = updated[index + 1]
                                                    updated[index + 1] = tmp
                                                    buttonOrder = updated
                                                    val enumList = updated.mapNotNull {
                                                        try { ActionButton.valueOf(if (it == "DEL") "DELETE" else it) } catch (_: Exception) { null }
                                                    }
                                                    viewModel.updateButtonOrder(enumList)
                                                }
                                            },
                                            enabled = index < buttonOrder.size - 1
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down",
                                                tint = if (index < buttonOrder.size - 1) Color.White else Color(0xFF4A5568)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val defaultOrder = listOf("CUT", "COPY", "DELETE", "PASTE", "MORE", "ENTER")
                                buttonOrder = defaultOrder
                                val enumList = defaultOrder.map { ActionButton.valueOf(it) }
                                viewModel.updateButtonOrder(enumList)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243048)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset to Default Order", color = Color(0xFF93C5FD))
                        }
                    }
                }
            }
        }
    }
}
