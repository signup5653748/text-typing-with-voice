package com.example.core.designsystem

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
