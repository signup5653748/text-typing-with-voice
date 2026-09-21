package com.example.presentation.categories

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsCategoriesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsCategoriesUiState())
    val uiState: StateFlow<SettingsCategoriesUiState> = _uiState.asStateFlow()
}
