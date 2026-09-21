package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.editor.dialogs.LanguageEmptyBox
import com.example.core.editor.dialogs.LanguageItemRow
import com.example.core.editor.dialogs.LanguageLoadingBox
import com.example.presentation.editor.EditorViewModel
import com.example.speech.SpeechRecognitionWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(viewModel: EditorViewModel) {
    val settings by viewModel.settings.collectAsState()
    var languages by remember { mutableStateOf<List<SpeechRecognitionWrapper.LanguagePack>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.speechWrapper.getSupportedLanguages { 
            languages = it
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
                        "Downloaded Voice Languages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFECEFF8)
                    )
                    Text(
                        "Showing real on-device offline languages installed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentLangs = languages
            if (currentLangs == null) {
                LanguageLoadingBox()
            } else if (currentLangs.isEmpty()) {
                LanguageEmptyBox()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentLangs) { lang ->
                        val isSelected = settings.voiceLanguage.equals(lang.languageCode, ignoreCase = true) ||
                                (settings.voiceLanguage.startsWith(lang.languageCode, ignoreCase = true))

                        LanguageItemRow(
                            lang = lang,
                            isSelected = isSelected,
                            onSelect = { viewModel.setLanguage(lang.languageCode) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
