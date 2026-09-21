package com.example.presentation.layoutsettings

import com.example.data.SettingsEntity

data class LayoutSettingsUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val buttonOrder: List<String> = listOf("CUT", "COPY", "DELETE", "PASTE", "SELECT_ALL", "ENTER")
)
