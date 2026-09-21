package com.example.presentation.speechsettings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TTSWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeechSettingsViewModel(
    application: Application,
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, SettingsRepository(application))

    private val speechWrapper = SpeechRecognitionWrapper(application)
    private val ttsWrapper = TTSWrapper(application)

    private val _uiState = MutableStateFlow(SpeechSettingsUiState())
    val uiState: StateFlow<SpeechSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
        loadSpeechData()
    }

    private fun loadSpeechData() {
        speechWrapper.getSupportedLanguages { packs ->
            _uiState.value = _uiState.value.copy(speechLanguages = packs)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val engines = ttsWrapper.getAvailableEngines()
            val locales = ttsWrapper.getAvailableVoicesOrLocales()
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(ttsEngines = engines, ttsLocales = locales)
            }
        }
    }

    fun updateTtsEngine(enginePkg: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsEnginePackage(enginePkg)
            ttsWrapper.setEngine(enginePkg)
        }
    }

    fun updateTtsLanguage(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsLanguage(langCode)
            ttsWrapper.setLanguage(langCode)
        }
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateVoiceLanguage(langCode)
            settingsRepo.updateTtsLanguage(langCode)
            ttsWrapper.setLanguage(langCode)
        }
    }
}
