package com.example.presentation.speechsettings

import android.speech.tts.TextToSpeech
import com.example.data.SettingsEntity
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TtsLanguageItem
import java.util.Locale

data class SpeechSettingsUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val speechLanguages: List<SpeechRecognitionWrapper.LanguagePack> = emptyList(),
    val ttsEngines: List<TextToSpeech.EngineInfo> = emptyList(),
    val ttsLocales: List<Locale> = emptyList(),
    val ttsLanguages: List<TtsLanguageItem> = emptyList()
)
