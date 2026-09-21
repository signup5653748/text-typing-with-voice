package com.example.presentation.generalsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GeneralSettingsViewModel(
    application: Application,
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, SettingsRepository(application))

    val uiState: StateFlow<GeneralSettingsUiState> = settingsRepo.settingsFlow
        .map { GeneralSettingsUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GeneralSettingsUiState()
        )

    fun updateTheme(name: String, bgHex: Long, textHex: Long, hlHex: Long) {
        viewModelScope.launch {
            settingsRepo.updateTheme(name, bgHex, textHex, hlHex)
        }
    }

    fun updateHighlightColor(hex: Long) {
        viewModelScope.launch {
            settingsRepo.updateHighlightColor(hex)
        }
    }

    fun updateBackgroundColor(hex: Long) {
        viewModelScope.launch {
            settingsRepo.updateBackgroundColor(hex)
        }
    }

    fun updateTextColor(hex: Long) {
        viewModelScope.launch {
            settingsRepo.updateTextColor(hex)
        }
    }

    fun updateTextSize(size: Float) {
        viewModelScope.launch {
            settingsRepo.updateTextSize(size)
        }
    }

    fun updateHideHeadingSymbols(hide: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateHideHeadingSymbols(hide)
        }
    }

    fun updateAlwaysInsertMicDirectly(always: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateAlwaysInsertMicDirectly(always)
        }
    }

    fun updateStartOnReadingScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateStartOnReadingScreen(enabled)
        }
    }
}
