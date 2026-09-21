package com.example.presentation.layoutsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionButton
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LayoutSettingsViewModel(
    application: Application,
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, SettingsRepository(application))

    private val _uiState = MutableStateFlow(LayoutSettingsUiState())
    val uiState: StateFlow<LayoutSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                val excluded = setOf("MORE", "REPLACE", "REP", "K", "P", "KB", "KB_LOCK", "TOP", "END", "JUMP", "JUMP_TO")
                val list = settings.buttonOrder.split(",").map { it.trim().uppercase() }
                    .filter { it.isNotBlank() && it !in excluded }
                val resolvedOrder = if (list.size < 6) {
                    listOf("CUT", "COPY", "DELETE", "PASTE", "SELECT_ALL", "ENTER")
                } else {
                    list.take(6)
                }
                _uiState.value = LayoutSettingsUiState(settings = settings, buttonOrder = resolvedOrder)
            }
        }
    }

    fun updateButtonSizeMultiplier(multiplier: Float) {
        viewModelScope.launch {
            settingsRepo.updateButtonSizeMultiplier(multiplier)
        }
    }

    fun updateArrowSize(size: Float) {
        viewModelScope.launch {
            settingsRepo.updateArrowSize(size)
        }
    }

    fun updateButtonOrder(order: List<String>) {
        _uiState.value = _uiState.value.copy(buttonOrder = order)
        val enumList = order.mapNotNull {
            try { ActionButton.valueOf(if (it == "DEL") "DELETE" else it) } catch (_: Exception) { null }
        }
        viewModelScope.launch {
            settingsRepo.updateButtonOrder(enumList)
        }
    }

    fun resetDefaultOrder() {
        val defaultOrder = listOf("CUT", "COPY", "DELETE", "PASTE", "MORE", "ENTER")
        _uiState.value = _uiState.value.copy(buttonOrder = defaultOrder)
        val enumList = defaultOrder.map { ActionButton.valueOf(it) }
        viewModelScope.launch {
            settingsRepo.updateButtonOrder(enumList)
        }
    }
}
