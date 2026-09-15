package com.example.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ThemePreset(
    val name: String,
    val bg: Long,
    val text: Long,
    val highlight: Long
)

val PRESET_THEMES = listOf(
    ThemePreset("Dark", 0xFF0C0D10, 0xFFECEEF2, 0xFFFFD600),
    ThemePreset("Light", 0xFFF5F7FA, 0xFF1C2028, 0xFF2563EB),
    ThemePreset("OLED Black", 0xFF000000, 0xFFFFFFFF, 0xFFFFD600),
    ThemePreset("Midnight Blue", 0xFF0B132B, 0xFFE0E6ED, 0xFF48CAE4),
    ThemePreset("Forest Green", 0xFF0F2018, 0xFFE2EFE9, 0xFF52B788),
    ThemePreset("Warm Sepia", 0xFF2C241D, 0xFFF3E9DC, 0xFFE07A5F)
)

val COLOR_OPTIONS = listOf(
    0xFFFFD600 to "Bright Yellow",
    0xFF2563EB to "Royal Blue",
    0xFF56D0DE to "Cyan Teal",
    0xFF32D796 to "Mint Green",
    0xFFFF6584 to "Coral Pink",
    0xFFA855F7 to "Purple",
    0xFFFF9800 to "Vibrant Orange",
    0xFFFFFFFF to "Pure White",
    0xFFECEEF2 to "Off White",
    0xFF9AA0AC to "Slate Gray",
    0xFF1C2028 to "Dark Charcoal",
    0xFF000000 to "OLED Black"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "General",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFECEFF8)
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161A24)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0C0D10))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Preset Selector Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Theme Preset",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFECEFF8),
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PRESET_THEMES.forEach { preset ->
                                val isSelected = settings.themeName == preset.name
                                Surface(
                                    onClick = {
                                        viewModel.updateTheme(preset.name, preset.bg, preset.text, preset.highlight)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF20293D),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF93C5FD)) else null,
                                    modifier = Modifier.width(115.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.bg)).border(0.5.dp, Color.Gray, CircleShape))
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.text)))
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(preset.highlight)))
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = preset.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Colors Card (Highlight, Background, Text)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Highlight Color Picker
                        ColorSelectorItem(
                            title = "Highlight Color",
                            subtitle = "Cursor position & active selection color",
                            selectedHex = settings.highlightColorHex,
                            onColorSelected = { viewModel.updateHighlightColor(it) }
                        )

                        HorizontalDivider(color = Color(0xFF243048))

                        // Background Color Picker
                        ColorSelectorItem(
                            title = "Background Color",
                            subtitle = "Canvas editor background tint",
                            selectedHex = settings.backgroundColorHex,
                            onColorSelected = { viewModel.updateBackgroundColor(it) }
                        )

                        HorizontalDivider(color = Color(0xFF243048))

                        // Text Color Picker
                        ColorSelectorItem(
                            title = "Text Color",
                            subtitle = "Editor typography font color",
                            selectedHex = settings.textColorHex,
                            onColorSelected = { viewModel.updateTextColor(it) }
                        )
                    }
                }
            }

            // Text Size Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Text Size",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFECEFF8),
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = Color(0xFF20293D),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${settings.textSizeSp.toInt()} sp",
                                    color = Color(0xFF56D0DE),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("A", fontSize = 12.sp, color = Color(0xFF8FA7D8), fontWeight = FontWeight.Bold)
                            Slider(
                                value = settings.textSizeSp,
                                onValueChange = { viewModel.updateTextSize(it) },
                                valueRange = 12.0f..36.0f,
                                steps = 24,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )
                            Text("A", fontSize = 24.sp, color = Color(0xFF8FA7D8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Headings Display Settings Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Hide heading symbols",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFECEFF8),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Visually hides leading ▫️ markers in rendered editor view without modifying saved file content.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8FA7D8)
                            )
                        }

                        Switch(
                            checked = settings.hideHeadingSymbols,
                            onCheckedChange = { viewModel.updateHideHeadingSymbols(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2563EB),
                                uncheckedThumbColor = Color(0xFF8FA7D8),
                                uncheckedTrackColor = Color(0xFF243048)
                            )
                        )
                    }
                }
            }

            // Always insert mic text directly Toggle Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Always insert mic text directly",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFECEFF8),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When ON, mic clicks always insert speech directly at the caret. When OFF, mic clicks with an active selection open the Voice Replace popup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8FA7D8)
                            )
                        }

                        Switch(
                            checked = settings.alwaysInsertMicDirectly,
                            onCheckedChange = { viewModel.updateAlwaysInsertMicDirectly(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2563EB),
                                uncheckedThumbColor = Color(0xFF8FA7D8),
                                uncheckedTrackColor = Color(0xFF243048)
                            )
                        )
                    }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFFECEFF8), fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8FA7D8))
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(selectedHex))
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            COLOR_OPTIONS.forEach { (hex, name) ->
                val isSelected = selectedHex == hex
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = name,
                            tint = if (hex == 0xFFFFFFFFL || hex == 0xFFFFD600L || hex == 0xFFECEEF2L) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
