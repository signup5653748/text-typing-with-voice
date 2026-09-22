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
        viewModelScope.launch {
            ttsWrapper.availableTtsLanguages.collect { langs ->
                if (langs.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        ttsLanguages = langs,
                        ttsLocales = langs.map { it.locale }
                    )
                }
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
            val ttsLangs = ttsWrapper.queryDownloadedTtsLanguages()
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    ttsEngines = engines,
                    ttsLanguages = ttsLangs,
                    ttsLocales = ttsLangs.map { it.locale }
                )
            }
        }
    }

    fun updateTtsEngine(enginePkg: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsEnginePackage(enginePkg)
            ttsWrapper.setEngine(enginePkg)
            loadSpeechData()
        }
    }

    fun updateTtsLanguage(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsLanguage(langCode)
            ttsWrapper.setLanguage(langCode)
            ttsWrapper.speakFeedback("TTS language updated")
        }
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateVoiceLanguage(langCode)
            settingsRepo.updateTtsLanguage(langCode)
            ttsWrapper.setLanguage(langCode)
            ttsWrapper.speakFeedback("Voice language updated")
        }
    }
}
