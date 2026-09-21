package com.example.presentation.advancedsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdvancedSettingsViewModel(
    application: Application,
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, SettingsRepository(application))

    private val _uiState = MutableStateFlow(AdvancedSettingsUiState())
    val uiState: StateFlow<AdvancedSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                val disabledSet = settings.disabledSpeechFeedbackButtons
                    .split(",")
                    .map { it.trim().uppercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                _uiState.value = AdvancedSettingsUiState(
                    settings = settings,
                    isAdvancedEnabled = settings.advancedSettingsEnabled,
                    disabledButtonsSet = disabledSet
                )
            }
        }
    }

    fun updateAdvancedSettingsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateAdvancedSettingsEnabled(enabled)
        }
    }

    fun updateDisabledSpeechFeedbackButtons(buttonIds: List<String>) {
        val stringVal = buttonIds.joinToString(",")
        viewModelScope.launch {
            settingsRepo.updateDisabledSpeechFeedbackButtons(stringVal)
        }
    }

    fun toggleSpeechFeedbackForButton(buttonId: String) {
        val current = _uiState.value.disabledButtonsSet.toMutableSet()
        val upper = buttonId.uppercase()
        if (current.contains(upper)) {
            current.remove(upper)
        } else {
            current.add(upper)
        }
        updateDisabledSpeechFeedbackButtons(current.toList())
    }
}
