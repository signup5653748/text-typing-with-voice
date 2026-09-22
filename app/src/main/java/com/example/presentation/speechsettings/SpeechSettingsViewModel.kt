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
                val previousLang = _uiState.value.settings.ttsLanguage
                _uiState.value = _uiState.value.copy(settings = settings)
                if (settings.ttsLanguage != previousLang || _uiState.value.ttsVoiceVariants.isEmpty()) {
                    loadVoiceVariants(settings.ttsLanguage)
                }
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

    fun loadSpeechData() {
        speechWrapper.getSupportedLanguages { packs ->
            _uiState.value = _uiState.value.copy(speechLanguages = packs)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val engines = ttsWrapper.getAvailableEngines()
            val ttsLangs = ttsWrapper.queryDownloadedTtsLanguages()
            val variants = ttsWrapper.getVoiceVariantsForLanguage(_uiState.value.settings.ttsLanguage)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    ttsEngines = engines,
                    ttsLanguages = ttsLangs,
                    ttsLocales = ttsLangs.map { it.locale },
                    ttsVoiceVariants = variants
                )
            }
        }
    }

    private fun loadVoiceVariants(langCode: String) {
        val variants = ttsWrapper.getVoiceVariantsForLanguage(langCode)
        _uiState.value = _uiState.value.copy(ttsVoiceVariants = variants)
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
            val variants = ttsWrapper.getVoiceVariantsForLanguage(langCode)
            _uiState.value = _uiState.value.copy(ttsVoiceVariants = variants)
            // If previous voice doesn't match new language, pick first available or clear
            if (variants.isNotEmpty() && variants.none { it.name == _uiState.value.settings.ttsVoiceName }) {
                val firstVariant = variants.firstOrNull { it.isDownloaded } ?: variants.first()
                settingsRepo.updateTtsVoiceName(firstVariant.name)
                ttsWrapper.setVoice(firstVariant.name)
            }
            ttsWrapper.speakFeedback("TTS language updated")
        }
    }

    fun updateTtsVoiceVariant(voiceName: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsVoiceName(voiceName)
            ttsWrapper.setVoice(voiceName)
            ttsWrapper.speakFeedback("Voice variant selected")
        }
    }

    fun previewVoiceVariant(variant: com.example.speech.TtsVoiceVariant) {
        ttsWrapper.previewVoice(variant)
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateVoiceLanguage(langCode)
            ttsWrapper.speakFeedback("Voice typing language set to $langCode")
        }
    }

    fun openVoiceDownloadSettings() {
        speechWrapper.openVoiceDownloadSettings(getApplication())
    }

    fun openTtsInstallSettings() {
        ttsWrapper.openTtsInstallSettings(getApplication())
    }
}
