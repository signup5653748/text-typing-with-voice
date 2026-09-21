package com.example.presentation.advancedsettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

private val actionButtons = listOf(
    ActionButtonItem("DELETE", "Delete Button (DEL)", "Spoken audio cue when deleting text or character", Icons.Default.DeleteOutline, Color(0xFFF87171)),
    ActionButtonItem("COPY", "Copy Button (COPY)", "Spoken audio confirmation when copying selection", Icons.Default.ContentCopy, Color(0xFF60A5FA)),
    ActionButtonItem("CUT", "Cut Button (CUT)", "Spoken audio cue when cutting selected text", Icons.Default.ContentCut, Color(0xFFFBBF24)),
    ActionButtonItem("PASTE", "Paste Button (PASTE)", "Spoken audio cue when pasting from clipboard", Icons.Default.ContentPaste, Color(0xFF34D399)),
    ActionButtonItem("SELECT_ALL", "Select All Button (ALL)", "Spoken feedback when selecting all document text", Icons.Default.SelectAll, Color(0xFFA78BFA)),
    ActionButtonItem("ENTER", "Enter / New Line Button", "Spoken feedback when pressing enter", Icons.Default.KeyboardReturn, Color(0xFF38BDF8)),
    ActionButtonItem("TOP", "Top of Document (TOP)", "Spoken audio when jumping to the beginning", Icons.Default.VerticalAlignTop, Color(0xFF818CF8)),
    ActionButtonItem("END", "End of Document (END)", "Spoken audio when jumping to the end", Icons.Default.VerticalAlignBottom, Color(0xFF818CF8)),
    ActionButtonItem("K", "K Mode Toggle (K)", "Character / Word navigation switch announcement", Icons.Default.TextFields, Color(0xFF4ADE80)),
    ActionButtonItem("P", "P Mode Toggle (P)", "Paragraph / Line navigation switch announcement", Icons.Default.FormatAlignLeft, Color(0xFF4ADE80)),
    ActionButtonItem("SEL", "SEL Selection Toggle", "Selection mode on / off announcement", Icons.Default.TouchApp, Color(0xFFF472B6)),
    ActionButtonItem("KB_LOCK", "Keyboard Lock Toggle (KB)", "Lock / Unlock keyboard announcement", Icons.Default.Lock, Color(0xFFF97316))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCustomLayout: () -> Unit,
    viewModel: AdvancedSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdvancedEnabled = uiState.isAdvancedEnabled
    val disabledButtonsSet = uiState.disabledButtonsSet

    Scaffold(
        containerColor = Color(0xFF0C0D10),
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings", fontWeight = FontWeight.SemiBold, color = Color(0xFFECEFF8)) },
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
            item(key = "advanced_master_toggle") {
                MasterToggleCard(
                    isEnabled = isAdvancedEnabled,
                    onToggle = { viewModel.updateAdvancedSettingsEnabled(it) }
                )
            }

            item(key = "advanced_controls_section") {
                AnimatedVisibility(
                    visible = isAdvancedEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CustomKeypadNavCard(onNavigate = onNavigateToCustomLayout)
                        GranularFeedbackCard(
                            disabledButtonsSet = disabledButtonsSet,
                            onToggle = { viewModel.toggleSpeechFeedbackForButton(it) },
                            onEnableAll = { viewModel.updateDisabledSpeechFeedbackButtons(emptyList()) },
                            onMuteAll = { viewModel.updateDisabledSpeechFeedbackButtons(actionButtons.map { it.id }) }
                        )
                    }
                }
            }

            if (!isAdvancedEnabled) {
                item(key = "advanced_info_footer") {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF161E30).copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF8FA7D8), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Turn on Advanced Controls switch above to unlock granular speech feedback rules and keypad customization.", color = Color(0xFF8FA7D8), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterToggleCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF8B5CF6).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Enable Advanced Controls", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold)
                    Text("Master toggle for granular speech & custom layouts", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)
                }
            }
            Switch(checked = isEnabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun CustomKeypadNavCard(onNavigate: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF3B82F6).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Customize Home Keypad Layout", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold)
                    Text("Rearrange and move buttons", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)
                }
            }
            Button(onClick = onNavigate, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Keypad Customizer", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GranularFeedbackCard(
    disabledButtonsSet: Set<String>,
    onToggle: (String) -> Unit,
    onEnableAll: () -> Unit,
    onMuteAll: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Button Speech Feedback Controls", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold)
            Text("Toggle spoken voice feedback for specific action buttons", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEnableAll, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Enable All", fontSize = 13.sp) }
                OutlinedButton(onClick = onMuteAll, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("Mute All", fontSize = 13.sp) }
            }
            HorizontalDivider(color = Color(0xFF243048))
            actionButtons.forEach { item ->
                val isSpeechEnabled = !disabledButtonsSet.contains(item.id.uppercase())
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSpeechEnabled) Color(0xFF20293D) else Color(0xFF141926),
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(item.id) }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(item.accentColor.copy(alpha = if (isSpeechEnabled) 0.2f else 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(item.icon, contentDescription = item.title, tint = if (isSpeechEnabled) item.accentColor else Color(0xFF64748B), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.title, fontWeight = FontWeight.SemiBold, color = if (isSpeechEnabled) Color(0xFFECEFF8) else Color(0xFF94A3B8), fontSize = 14.sp)
                                Text(item.subtitle, color = if (isSpeechEnabled) Color(0xFF8FA7D8) else Color(0xFF64748B), fontSize = 11.5.sp)
                            }
                        }
                        Switch(checked = isSpeechEnabled, onCheckedChange = { onToggle(item.id) })
                    }
                }
            }
        }
    }
}
