package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.editor.dialogs.AdditionalLanguagesHeader
import com.example.core.editor.dialogs.LanguageEmptyBox
import com.example.core.editor.dialogs.LanguageItemRow
import com.example.core.editor.dialogs.LanguageLoadingBox
import com.example.core.editor.dialogs.SpeechDownloadHelpCard
import com.example.core.editor.dialogs.TtsDownloadHelpCard
import com.example.core.editor.dialogs.TtsLanguageItemRow
import com.example.core.editor.dialogs.VoiceVariantRow
import com.example.presentation.editor.EditorViewModel
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TtsLanguageItem
import com.example.speech.TtsVoiceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(viewModel: EditorViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Text-to-Speech (TTS), 1: Voice Typing
    var speechLanguages by remember { mutableStateOf<List<SpeechRecognitionWrapper.LanguagePack>?>(null) }
    var ttsLanguages by remember { mutableStateOf<List<TtsLanguageItem>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isAdditionalTtsExpanded by remember { mutableStateOf(false) }
    var isAdditionalSpeechExpanded by remember { mutableStateOf(false) }

    // Two-step TTS selection: when non-null, showing voice variants for this language
    var selectedTtsForVariants by remember { mutableStateOf<TtsLanguageItem?>(null) }
    var voiceVariants by remember { mutableStateOf<List<TtsVoiceVariant>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Query Speech recognition languages
        viewModel.speechWrapper.getSupportedLanguages { 
            speechLanguages = it
        }
        // Query real TTS downloaded languages
        val list = viewModel.ttsWrapper.queryDownloadedTtsLanguages()
        ttsLanguages = list
    }

    LaunchedEffect(selectedTtsForVariants) {
        val target = selectedTtsForVariants
        if (target != null) {
            voiceVariants = viewModel.getVoiceVariantsForLanguage(target.languageTag)
        } else {
            voiceVariants = emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeLanguagePicker() },
        containerColor = Color(0xFF131A2A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            if (selectedTtsForVariants != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { selectedTtsForVariants = null }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to languages",
                            tint = Color(0xFF56D0DE)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedTtsForVariants?.displayName ?: "Voice Variants",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8)
                        )
                        Text(
                            "Step 2: Choose Voice Variant & Quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
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
                            "Voice & Speech Languages",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8)
                        )
                        Text(
                            "Choose read-aloud voices & voice typing languages",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs (Only when not in variant drill-down)
            if (selectedTtsForVariants == null) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1B2438),
                    contentColor = Color(0xFF56D0DE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            searchQuery = ""
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("TTS Voices", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            searchQuery = ""
                        },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (selectedTab == 0) "Search TTS languages..." else "Search typing languages...",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF161E30),
                        unfocusedContainerColor = Color(0xFF161E30),
                        focusedBorderColor = Color(0xFF56D0DE),
                        unfocusedBorderColor = Color(0xFF233554),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFECEFF8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Tab Content
            if (selectedTab == 0) {
                // TTS TAB
                if (selectedTtsForVariants != null) {
                    // STEP 2: VOICE VARIANTS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (voiceVariants.isEmpty()) {
                            item {
                                LanguageEmptyBox(message = "Standard system voice will be used for this language.")
                            }
                        } else {
                            items(voiceVariants) { variant ->
                                val isSelected = settings.ttsVoiceName == variant.name ||
                                        (settings.ttsVoiceName.isBlank() && voiceVariants.firstOrNull() == variant)

                                VoiceVariantRow(
                                    variant = variant,
                                    isSelected = isSelected,
                                    onSelect = {
                                        viewModel.updateTtsVoiceVariant(variant.name)
                                        viewModel.closeLanguagePicker()
                                    },
                                    onPreview = {
                                        viewModel.previewVoiceVariant(variant)
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            TtsDownloadHelpCard(
                                onOpenTtsSettings = {
                                    viewModel.openTtsInstallSettings(context)
                                }
                            )
                        }
                    }
                } else {
                    // STEP 1: PICK TTS LANGUAGE
                    val currentTts = ttsLanguages
                    if (currentTts == null) {
                        LanguageLoadingBox()
                    } else {
                        val filteredList = if (searchQuery.isBlank()) {
                            currentTts
                        } else {
                            currentTts.filter {
                                it.displayName.contains(searchQuery, ignoreCase = true) ||
                                        it.languageTag.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        if (filteredList.isEmpty()) {
                            LanguageEmptyBox(message = "No matching TTS languages found.")
                        } else {
                            val downloadedList = filteredList.filter { it.isDownloaded }
                            val additionalList = filteredList.filter { !it.isDownloaded }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Downloaded Languages First
                                if (downloadedList.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Downloaded Languages",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(downloadedList) { item ->
                                        val isSelected = settings.ttsLanguage.equals(item.languageTag, ignoreCase = true) ||
                                                (settings.ttsLanguage.startsWith(item.languageTag, ignoreCase = true))

                                        TtsLanguageItemRow(
                                            item = item,
                                            isSelected = isSelected,
                                            onSelect = {
                                                viewModel.updateTtsLanguage(item.languageTag)
                                                selectedTtsForVariants = item
                                            }
                                        )
                                    }
                                }

                                // 2. Additional Languages Dropdown
                                if (additionalList.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        AdditionalLanguagesHeader(
                                            count = additionalList.size,
                                            isExpanded = isAdditionalTtsExpanded || searchQuery.isNotBlank(),
                                            onToggle = { isAdditionalTtsExpanded = !isAdditionalTtsExpanded },
                                            title = "Additional Languages",
                                            subtitle = "Download voice data or pick language"
                                        )
                                    }

                                    if (isAdditionalTtsExpanded || searchQuery.isNotBlank()) {
                                        items(additionalList) { item ->
                                            val isSelected = settings.ttsLanguage.equals(item.languageTag, ignoreCase = true) ||
                                                    (settings.ttsLanguage.startsWith(item.languageTag, ignoreCase = true))

                                            TtsLanguageItemRow(
                                                item = item,
                                                isSelected = isSelected,
                                                onSelect = {
                                                    viewModel.updateTtsLanguage(item.languageTag)
                                                    selectedTtsForVariants = item
                                                },
                                                onDownloadVoice = {
                                                    viewModel.openTtsInstallSettings(context)
                                                }
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TtsDownloadHelpCard(
                                        onOpenTtsSettings = {
                                            viewModel.openTtsInstallSettings(context)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // VOICE TYPING TAB
                val currentSpeech = speechLanguages
                if (currentSpeech == null) {
                    LanguageLoadingBox()
                } else {
                    val filteredSpeech = if (searchQuery.isBlank()) {
                        currentSpeech
                    } else {
                        currentSpeech.filter {
                            it.displayName.contains(searchQuery, ignoreCase = true) ||
                                    it.languageCode.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredSpeech.isEmpty()) {
                        LanguageEmptyBox(message = "No matching voice typing languages found.")
                    } else {
                        val downloadedSpeech = filteredSpeech.filter { it.isOfflineAvailable }
                        val additionalSpeech = filteredSpeech.filter { !it.isOfflineAvailable }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                SpeechDownloadHelpCard(
                                    onOpenSettings = {
                                        viewModel.openVoiceDownloadSettings(context)
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // 1. Downloaded Speech Languages First
                            if (downloadedSpeech.isNotEmpty()) {
                                item {
                                    Text(
                                        "Downloaded Languages",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(downloadedSpeech) { lang ->
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

                            // 2. Additional Languages Dropdown
                            if (additionalSpeech.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AdditionalLanguagesHeader(
                                        count = additionalSpeech.size,
                                        isExpanded = isAdditionalSpeechExpanded || searchQuery.isNotBlank(),
                                        onToggle = { isAdditionalSpeechExpanded = !isAdditionalSpeechExpanded },
                                        title = "Additional Languages",
                                        subtitle = "Download offline dictionary or pick language"
                                    )
                                }

                                if (isAdditionalSpeechExpanded || searchQuery.isNotBlank()) {
                                    items(additionalSpeech) { lang ->
                                        val isSelected = settings.voiceLanguage.equals(lang.languageCode, ignoreCase = true) ||
                                                (settings.voiceLanguage.startsWith(lang.languageCode, ignoreCase = true))

                                        LanguageItemRow(
                                            lang = lang,
                                            isSelected = isSelected,
                                            onSelect = {
                                                viewModel.setLanguage(lang.languageCode)
                                                viewModel.closeLanguagePicker()
                                            },
                                            onDownloadDictionary = {
                                                viewModel.downloadSpeechDictionary(lang.languageCode, context)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
