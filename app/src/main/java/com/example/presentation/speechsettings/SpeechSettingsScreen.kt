package com.example.presentation.speechsettings

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.core.editor.dialogs.SpeechDownloadHelpCard
import com.example.core.editor.dialogs.TtsDownloadHelpCard
import com.example.core.editor.dialogs.VoiceVariantRow
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
                title = { Text("Speech & Voice Settings", fontWeight = FontWeight.SemiBold, color = Color(0xFFECEFF8)) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    ttsLanguages = uiState.ttsLanguages,
                    ttsLocales = uiState.ttsLocales,
                    selectedTtsLanguage = settings.ttsLanguage,
                    onLanguageSelected = { viewModel.updateTtsLanguage(it) },
                    onOpenTtsSettings = { viewModel.openTtsInstallSettings() }
                )
            }

            if (uiState.ttsVoiceVariants.isNotEmpty()) {
                item(key = "tts_voice_variants_card") {
                    TtsVoiceVariantsCard(
                        variants = uiState.ttsVoiceVariants,
                        selectedVoiceName = settings.ttsVoiceName,
                        onVariantSelected = { viewModel.updateTtsVoiceVariant(it) },
                        onPreview = { viewModel.previewVoiceVariant(it) }
                    )
                }
            }

            item(key = "voice_typing_language_card") {
                VoiceTypingLanguageCard(
                    speechLanguages = uiState.speechLanguages,
                    selectedVoiceLanguage = settings.voiceLanguage,
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onOpenDownloadSettings = { viewModel.openVoiceDownloadSettings() }
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
    ttsLanguages: List<com.example.speech.TtsLanguageItem>,
    ttsLocales: List<Locale>,
    selectedTtsLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onOpenTtsSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Step 1: Pick TTS Language", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                    Text("Choose language to read aloud", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)
                }
            }

            val availableVoiceItems = remember(ttsLanguages, ttsLocales) {
                if (ttsLanguages.isNotEmpty()) {
                    ttsLanguages.map { item ->
                        Triple(item.languageTag, item.displayName, item.isDownloaded)
                    }
                } else {
                    val list = ttsLocales.ifEmpty { listOf(Locale.getDefault()) }
                    val defaultLoc = Locale.getDefault()
                    list.distinctBy { it.language to it.country }.map { loc ->
                        val tag = loc.toLanguageTag().ifBlank { "${loc.language}-${loc.country}".trimEnd('-') }
                        val label = loc.getDisplayName(defaultLoc).ifBlank { loc.displayName }.replaceFirstChar { it.uppercase() }
                        Triple(tag, label, true)
                    }
                }
            }

            if (availableVoiceItems.isEmpty()) {
                Text("No TTS languages found on system", color = Color(0xFF64748B), fontSize = 13.sp)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableVoiceItems.forEach { (tag, labelName, isDownloaded) ->
                        val isSelected = selectedTtsLanguage == tag || (selectedTtsLanguage.isEmpty() && tag.startsWith("en", ignoreCase = true))
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLanguageSelected(tag) },
                            label = {
                                Text(
                                    if (isDownloaded) "$labelName ✓" else labelName,
                                    fontSize = 12.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF20293D),
                                labelColor = Color(0xFFECEFF8)
                            )
                        )
                    }
                }
            }

            TtsDownloadHelpCard(onOpenTtsSettings = onOpenTtsSettings)
        }
    }
}

@Composable
private fun TtsVoiceVariantsCard(
    variants: List<com.example.speech.TtsVoiceVariant>,
    selectedVoiceName: String,
    onVariantSelected: (String) -> Unit,
    onPreview: (com.example.speech.TtsVoiceVariant) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Step 2: Choose Voice Variant", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
            Text("Select pitch, gender & timbre (tap speaker icon to preview audio)", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                variants.forEach { variant ->
                    val isSelected = selectedVoiceName == variant.name || (selectedVoiceName.isEmpty() && variants.firstOrNull() == variant)
                    VoiceVariantRow(
                        variant = variant,
                        isSelected = isSelected,
                        onSelect = { onVariantSelected(variant.name) },
                        onPreview = { onPreview(variant) }
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
    onLanguageSelected: (String) -> Unit,
    onOpenDownloadSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Voice Typing Recognition Language", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
            Text("Select active voice dictation language", color = Color(0xFF8FA7D8), fontSize = 12.5.sp)

            SpeechDownloadHelpCard(onOpenSettings = onOpenDownloadSettings)

            if (speechLanguages.isEmpty()) {
                Text("No voice languages detected", color = Color(0xFF64748B), fontSize = 13.sp)
            } else {
                Text("All Available Languages:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speechLanguages.forEach { lang ->
                        val isSelected = selectedVoiceLanguage.equals(lang.languageCode, ignoreCase = true) ||
                                (selectedVoiceLanguage.startsWith(lang.languageCode, ignoreCase = true))
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLanguageSelected(lang.languageCode) },
                            label = {
                                Text(
                                    lang.displayName,
                                    fontSize = 12.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF059669),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF20293D),
                                labelColor = Color(0xFFECEFF8)
                            )
                        )
                    }
                }
            }
        }
    }
}
