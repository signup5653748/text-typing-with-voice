package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    var languages by remember { mutableStateOf<List<com.example.speech.SpeechRecognitionWrapper.LanguagePack>>(emptyList()) }
    LaunchedEffect(Unit) {
        viewModel.speechWrapper.getSupportedLanguages { 
            languages = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Button size", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Small")
                    Slider(
                        value = settings.arrowSize,
                        onValueChange = { viewModel.updateArrowSize(it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                    Text("Large")
                }
            }
            
            item {
                Text("Customize - Arrange buttons", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            val currentOrder = settings.buttonOrder.split(",").mapNotNull { 
                try { ActionButton.valueOf(it) } catch (e: Exception) { null }
            }.toMutableList()

            if (currentOrder.isEmpty()) {
                currentOrder.addAll(listOf(ActionButton.CUT, ActionButton.COPY, ActionButton.K, ActionButton.P, ActionButton.DELETE, ActionButton.PASTE, ActionButton.ENTER))
            }

            items(currentOrder.size, key = { currentOrder[it].name }) { index ->
                val btn = currentOrder[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(btn.name, style = MaterialTheme.typography.bodyLarge)
                    Row {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val temp = currentOrder[index]
                                    currentOrder[index] = currentOrder[index - 1]
                                    currentOrder[index - 1] = temp
                                    viewModel.updateButtonOrder(currentOrder)
                                }
                            },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "Move Up")
                        }
                        IconButton(
                            onClick = {
                                if (index < currentOrder.size - 1) {
                                    val temp = currentOrder[index]
                                    currentOrder[index] = currentOrder[index + 1]
                                    currentOrder[index + 1] = temp
                                    viewModel.updateButtonOrder(currentOrder)
                                }
                            },
                            enabled = index < currentOrder.size - 1
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Move Down")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Voice language", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(languages) { lang ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setLanguage(lang.languageCode) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.voiceLanguage == lang.languageCode,
                        onClick = { viewModel.setLanguage(lang.languageCode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(lang.displayName, style = MaterialTheme.typography.bodyLarge)
                        if (lang.isOfflineAvailable) {
                            Text("Offline available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
