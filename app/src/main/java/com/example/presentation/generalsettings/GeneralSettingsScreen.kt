package com.example.presentation.generalsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.designsystem.COLOR_OPTIONS
import com.example.core.designsystem.PRESET_THEMES
import com.example.widget.ReadingModeWidgetProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: GeneralSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFF0C0D10),
        topBar = {
            TopAppBar(
                title = {
                    Text("General", fontWeight = FontWeight.SemiBold, color = Color(0xFFECEFF8))
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
            item(key = "theme_preset_card") {
                ThemePresetCard(
                    selectedTheme = settings.themeName,
                    onThemeSelected = { name, bg, text, hl -> viewModel.updateTheme(name, bg, text, hl) }
                )
            }

            item(key = "colors_card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ColorSelectorItem("Highlight Color", "Cursor & selection", settings.highlightColorHex) {
                            viewModel.updateHighlightColor(it)
                        }
                        HorizontalDivider(color = Color(0xFF243048))
                        ColorSelectorItem("Background Color", "Canvas tint", settings.backgroundColorHex) {
                            viewModel.updateBackgroundColor(it)
                        }
                        HorizontalDivider(color = Color(0xFF243048))
                        ColorSelectorItem("Text Color", "Font color", settings.textColorHex) {
                            viewModel.updateTextColor(it)
                        }
                    }
                }
            }

            item(key = "text_size_card") {
                TextSizeCard(
                    textSizeSp = settings.textSizeSp,
                    onTextSizeChanged = { viewModel.updateTextSize(it) }
                )
            }

            item(key = "toggles_card") {
                SettingsTogglesCard(
                    hideHeadingSymbols = settings.hideHeadingSymbols,
                    onHideHeadingsChanged = { viewModel.updateHideHeadingSymbols(it) },
                    alwaysInsertMicDirectly = settings.alwaysInsertMicDirectly,
                    onAlwaysInsertMicChanged = { viewModel.updateAlwaysInsertMicDirectly(it) },
                    startOnReadingScreen = settings.startOnReadingScreen,
                    onStartOnReadingScreenChanged = { viewModel.updateStartOnReadingScreen(it) },
                    onAddWidgetClicked = { ReadingModeWidgetProvider.requestPinWidget(context) }
                )
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    selectedTheme: String,
    onThemeSelected: (String, Long, Long, Long) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Theme Preset", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PRESET_THEMES.forEach { preset ->
                    val isSelected = selectedTheme == preset.name
                    Surface(
                        onClick = { onThemeSelected(preset.name, preset.bg, preset.text, preset.highlight) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF20293D),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF93C5FD)) else null,
                        modifier = Modifier.width(115.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.bg)).border(0.5.dp, Color.Gray, CircleShape))
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.text)))
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.highlight)))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(preset.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextSizeCard(
    textSizeSp: Float,
    onTextSizeChanged: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Text Size", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                Surface(color = Color(0xFF20293D), shape = RoundedCornerShape(8.dp)) {
                    Text("${textSizeSp.toInt()} sp", color = Color(0xFF56D0DE), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", fontSize = 12.sp, color = Color(0xFF8FA7D8), fontWeight = FontWeight.Bold)
                Slider(value = textSizeSp, onValueChange = onTextSizeChanged, valueRange = 12.0f..36.0f, steps = 24, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                Text("A", fontSize = 24.sp, color = Color(0xFF8FA7D8), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsTogglesCard(
    hideHeadingSymbols: Boolean,
    onHideHeadingsChanged: (Boolean) -> Unit,
    alwaysInsertMicDirectly: Boolean,
    onAlwaysInsertMicChanged: (Boolean) -> Unit,
    startOnReadingScreen: Boolean,
    onStartOnReadingScreenChanged: (Boolean) -> Unit,
    onAddWidgetClicked: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Hide heading symbols", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                    Text("Hides leading ▫️ markers in editor view without modifying saved file.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8FA7D8))
                }
                Switch(checked = hideHeadingSymbols, onCheckedChange = onHideHeadingsChanged)
            }
            HorizontalDivider(color = Color(0xFF243048))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Always insert mic text directly", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                    Text("Insert speech directly at caret instead of opening replace popup.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8FA7D8))
                }
                Switch(checked = alwaysInsertMicDirectly, onCheckedChange = onAlwaysInsertMicChanged)
            }
            HorizontalDivider(color = Color(0xFF243048))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Start on Reading Screen", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                    Text("Automatically visit Reading Screen when launching or visiting home.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8FA7D8))
                }
                Switch(checked = startOnReadingScreen, onCheckedChange = onStartOnReadingScreenChanged)
            }
            HorizontalDivider(color = Color(0xFF243048))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Home Screen Reading Widget", style = MaterialTheme.typography.titleMedium, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                    Text("Add a quick visit and read widget to your phone's home screen.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8FA7D8))
                }
                Button(
                    onClick = onAddWidgetClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = "Add Widget", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }
    }
}

@Composable
private fun ColorSelectorItem(
    title: String,
    subtitle: String,
    selectedHex: Long,
    onColorSelected: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8FA7D8))
            }
            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(selectedHex)).border(1.5.dp, Color.White, CircleShape))
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            COLOR_OPTIONS.forEach { (hex, name) ->
                val isSelected = selectedHex == hex
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(hex))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFF334155), CircleShape)
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = name, tint = if (hex == 0xFFFFFFFFL || hex == 0xFFFFD600L || hex == 0xFFECEEF2L) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
