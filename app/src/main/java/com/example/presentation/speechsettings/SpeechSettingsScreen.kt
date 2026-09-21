package com.example.presentation.speechsettings

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpeechSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    Scaffold(
        containerColor = Color(0xFF0C0D10),
        topBar = {
            TopAppBar(
                title = { Text("Speech", fontWeight = FontWeight.SemiBold, color = Color(0xFFECEFF8)) },
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
            if (uiState.ttsEngines.isNotEmpty()) {
                item(key = "tts_engines_card") {
                    TtsEnginesCard(
                        engines = uiState.ttsEngines,
                        selectedEngine = settings.ttsEnginePackage,
                        onEngineSelected = { viewModel.updateTtsEngine(it) }
                    )
                }
            }

            item(key = "tts_language_card") {
                TtsLanguageCard(
                    ttsLocales = uiState.ttsLocales,
                    selectedTtsLanguage = settings.ttsLanguage,
                    onLanguageSelected = { viewModel.updateTtsLanguage(it) }
                )
            }

            item(key = "voice_typing_language_card") {
                VoiceTypingLanguageCard(
                    speechLanguages = uiState.speechLanguages,
                    selectedVoiceLanguage = settings.voiceLanguage,
                    onLanguageSelected = { viewModel.setLanguage(it) }
                )
            }
        }
    }
}

@Composable
private fun TtsEnginesCard(
    engines: List<TextToSpeech.EngineInfo>,
    selectedEngine: String,
    onEngineSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Text-to-Speech Engine", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
            Text("Select the system synthesis engine for read-aloud playback", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                engines.forEach { engine ->
                    val isSelected = selectedEngine == engine.name || (selectedEngine.isEmpty() && engine.name.contains("google", ignoreCase = true))
                    Surface(
                        onClick = { onEngineSelected(engine.name) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.35f) else Color(0xFF20293D),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA)) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onEngineSelected(engine.name) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF60A5FA), unselectedColor = Color(0xFF8FA7D8))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(engine.label.ifBlank { engine.name }, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(engine.name, color = Color(0xFF8FA7D8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TtsLanguageCard(
    ttsLocales: List<Locale>,
    selectedTtsLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Text-to-Speech Language", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
            Text("Voice language used when reading document text aloud", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)

            val availableVoiceItems = remember(ttsLocales) {
                val list = ttsLocales.ifEmpty { listOf(Locale.getDefault()) }
                val defaultLoc = Locale.getDefault()
                list.distinctBy { it.language to it.country }.map { loc ->
                    val tag = loc.toLanguageTag().ifBlank { "${loc.language}-${loc.country}".trimEnd('-') }
                    val label = loc.getDisplayName(defaultLoc).ifBlank { loc.displayName }.replaceFirstChar { it.uppercase() }
                    tag to label
                }
            }

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableVoiceItems.forEach { (tag, labelName) ->
                    val isSelected = selectedTtsLanguage == tag || (selectedTtsLanguage.isEmpty() && tag.startsWith("en", ignoreCase = true))
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLanguageSelected(tag) },
                        label = { Text(labelName, fontSize = 12.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF2563EB), selectedLabelColor = Color.White, containerColor = Color(0xFF20293D), labelColor = Color(0xFFECEFF8))
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceTypingLanguageCard(
    speechLanguages: List<com.example.speech.SpeechRecognitionWrapper.LanguagePack>,
    selectedVoiceLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Voice Typing Recognition Language", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
            Text("Actual installed/downloaded offline recognition languages", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)

            if (speechLanguages.isEmpty()) {
                Text("No downloaded offline voice packs detected", color = Color(0xFF64748B), fontSize = 13.sp)
            } else {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    speechLanguages.forEach { lang ->
                        val isSelected = selectedVoiceLanguage == lang.languageCode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLanguageSelected(lang.languageCode) },
                            label = { Text(lang.displayName, fontSize = 12.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF059669), selectedLabelColor = Color.White, containerColor = Color(0xFF20293D), labelColor = Color(0xFFECEFF8))
                        )
                    }
                }
            }
        }
    }
}
