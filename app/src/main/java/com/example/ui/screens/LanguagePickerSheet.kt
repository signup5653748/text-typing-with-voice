package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.speech.SpeechRecognitionWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(viewModel: EditorViewModel) {
    val settings by viewModel.settings.collectAsState()
    var languages by remember { mutableStateOf<List<SpeechRecognitionWrapper.LanguagePack>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.speechWrapper.getSupportedLanguages { 
            languages = it
        }
    }

    ModalBottomSheet(onDismissRequest = { viewModel.closeLanguagePicker() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Select Voice Language", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (languages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn {
                    items(languages) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setLanguage(lang.languageCode) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lang.displayName, style = MaterialTheme.typography.bodyLarge)
                                if (lang.isOfflineAvailable) {
                                    Text("Offline available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (settings.voiceLanguage == lang.languageCode) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
