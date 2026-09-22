package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.editor.dialogs.LanguageEmptyBox
import com.example.core.editor.dialogs.LanguageItemRow
import com.example.core.editor.dialogs.LanguageLoadingBox
import com.example.core.editor.dialogs.TtsLanguageItemRow
import com.example.presentation.editor.EditorViewModel
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TtsLanguageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(viewModel: EditorViewModel) {
    val settings by viewModel.settings.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Text-to-Speech (TTS), 1: Voice Typing
    var speechLanguages by remember { mutableStateOf<List<SpeechRecognitionWrapper.LanguagePack>?>(null) }
    var ttsLanguages by remember { mutableStateOf<List<TtsLanguageItem>?>(null) }

    LaunchedEffect(Unit) {
        // Query Speech recognition languages
        viewModel.speechWrapper.getSupportedLanguages { 
            speechLanguages = it
        }
        // Query real TTS downloaded languages
        ttsLanguages = viewModel.ttsWrapper.queryDownloadedTtsLanguages()
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeLanguagePicker() },
        containerColor = Color(0xFF131A2A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = Color(0xFF56D0DE),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        "Installed & Downloaded Languages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFECEFF8)
                    )
                    Text(
                        "On-device voice and speech engines",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs (TTS vs Voice Typing)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1B2438),
                contentColor = Color(0xFF56D0DE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("TTS Read Aloud", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Voice Typing", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // TTS Languages
                val currentTts = ttsLanguages
                if (currentTts == null) {
                    LanguageLoadingBox()
                } else if (currentTts.isEmpty()) {
                    LanguageEmptyBox(message = "No downloaded TTS voices found.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentTts) { item ->
                            val isSelected = settings.ttsLanguage.equals(item.languageTag, ignoreCase = true) ||
                                    (settings.ttsLanguage.startsWith(item.languageTag, ignoreCase = true))

                            TtsLanguageItemRow(
                                item = item,
                                isSelected = isSelected,
                                onSelect = {
                                    viewModel.updateTtsLanguage(item.languageTag)
                                    viewModel.closeLanguagePicker()
                                }
                            )
                        }
                    }
                }
            } else {
                // Voice Typing Languages
                val currentSpeech = speechLanguages
                if (currentSpeech == null) {
                    LanguageLoadingBox()
                } else if (currentSpeech.isEmpty()) {
                    LanguageEmptyBox(message = "No offline voice typing packs detected.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(currentSpeech) { lang ->
                            val isSelected = settings.voiceLanguage.equals(lang.languageCode, ignoreCase = true) ||
                                    (settings.voiceLanguage.startsWith(lang.languageCode, ignoreCase = true))

                            LanguageItemRow(
                                lang = lang,
                                isSelected = isSelected,
                                onSelect = {
                                    viewModel.setLanguage(lang.languageCode)
                                    viewModel.closeLanguagePicker()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
