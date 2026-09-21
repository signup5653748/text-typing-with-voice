package com.example.presentation.layoutsettings

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
import androidx.lifecycle.viewmodel.compose.viewModel

private val availableSlotActions = listOf(
    "CUT" to "Cut (CUT)",
    "COPY" to "Copy (COPY)",
    "DELETE" to "Delete (DEL)",
    "PASTE" to "Paste (PASTE)",
    "SELECT_ALL" to "Select All (ALL)",
    "ENTER" to "Enter Line (ENTER)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LayoutSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val buttonOrder = uiState.buttonOrder

    Scaffold(
        containerColor = Color(0xFF0C0D10),
        topBar = {
            TopAppBar(
                title = { Text("Layout", fontWeight = FontWeight.SemiBold, color = Color(0xFFECEFF8)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF8FA7D8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161A24))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "button_size_card") {
                ButtonScalingCard(
                    buttonMultiplier = settings.buttonSizeMultiplier,
                    onButtonMultiplierChange = { viewModel.updateButtonSizeMultiplier(it) },
                    arrowSize = settings.arrowSize,
                    onArrowSizeChange = { viewModel.updateArrowSize(it) }
                )
            }

            item(key = "button_arrangement_card") {
                ButtonArrangementCard(
                    buttonOrder = buttonOrder,
                    onOrderChanged = { viewModel.updateButtonOrder(it) },
                    onResetDefault = { viewModel.resetDefaultOrder() }
                )
            }
        }
    }
}

@Composable
private fun ButtonScalingCard(
    buttonMultiplier: Float,
    onButtonMultiplierChange: (Float) -> Unit,
    arrowSize: Float,
    onArrowSizeChange: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Action Button Size", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                    Surface(color = Color(0xFF20293D), shape = RoundedCornerShape(8.dp)) {
                        Text("${(buttonMultiplier * 100).toInt()}%", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Compact", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                    Slider(value = buttonMultiplier, onValueChange = onButtonMultiplierChange, valueRange = 0.8f..1.5f, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    Text("Large", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                }
            }

            HorizontalDivider(color = Color(0xFF243048))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("D-Pad Arrow Cluster Size", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                    Surface(color = Color(0xFF20293D), shape = RoundedCornerShape(8.dp)) {
                        Text("${(arrowSize * 100).toInt()}%", color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Small", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                    Slider(value = arrowSize, onValueChange = onArrowSizeChange, valueRange = 0.7f..1.6f, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    Text("Large", fontSize = 12.sp, color = Color(0xFF8FA7D8))
                }
            }
        }
    }
}

@Composable
private fun ButtonArrangementCard(
    buttonOrder: List<String>,
    onOrderChanged: (List<String>) -> Unit,
    onResetDefault: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Custom Button Arrangement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Shift any action button into your preferred keypad slot position.", fontSize = 12.5.sp, color = Color(0xFF8FA7D8))

            buttonOrder.forEachIndexed { index, actionName ->
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF20293D), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF7C3AED)), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(availableSlotActions.find { it.first == actionName }?.second ?: actionName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val updated = buttonOrder.toMutableList()
                                        val tmp = updated[index]
                                        updated[index] = updated[index - 1]
                                        updated[index - 1] = tmp
                                        onOrderChanged(updated)
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (index > 0) Color.White else Color(0xFF4A5568))
                            }

                            IconButton(
                                onClick = {
                                    if (index < buttonOrder.size - 1) {
                                        val updated = buttonOrder.toMutableList()
                                        val tmp = updated[index]
                                        updated[index] = updated[index + 1]
                                        updated[index + 1] = tmp
                                        onOrderChanged(updated)
                                    }
                                },
                                enabled = index < buttonOrder.size - 1
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (index < buttonOrder.size - 1) Color.White else Color(0xFF4A5568))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onResetDefault,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243048)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Default Order", color = Color(0xFF93C5FD))
            }
        }
    }
}
