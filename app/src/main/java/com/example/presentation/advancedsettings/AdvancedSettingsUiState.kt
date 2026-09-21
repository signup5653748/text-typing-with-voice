package com.example.presentation.advancedsettings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.SettingsEntity

data class ActionButtonItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

data class AdvancedSettingsUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val isAdvancedEnabled: Boolean = false,
    val disabledButtonsSet: Set<String> = emptySet()
)
