package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognitionWrapper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialResults = MutableStateFlow("")
    val partialResults: StateFlow<String> = _partialResults.asStateFlow()

    private val _finalResult = MutableStateFlow<String?>(null)
    val finalResult: StateFlow<String?> = _finalResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startListening(languageCode: String) {
        if (_isListening.value) return

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }
                override fun onError(error: Int) {
                    _error.value = "Error code: $error"
                    _isListening.value = false
                    release()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _finalResult.value = matches[0]
                    }
                    _isListening.value = false
                    release()
                }
                override fun onPartialResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _partialResults.value = matches[0]
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        _partialResults.value = ""
        _finalResult.value = null
        _error.value = null
        _isListening.value = true

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        if (_isListening.value) {
            speechRecognizer?.stopListening()
            _isListening.value = false
        }
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isListening.value = false
    }

    fun clearResults() {
        _partialResults.value = ""
        _finalResult.value = null
        _error.value = null
    }

    fun getSupportedLanguages(callback: (List<LanguagePack>) -> Unit) {
        val defaultList = listOf(
            LanguagePack("English (US)", "en-US", false),
            LanguagePack("Spanish", "es-ES", false),
            LanguagePack("French", "fr-FR", false),
            LanguagePack("German", "de-DE", false),
            LanguagePack("Japanese", "ja-JP", false)
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer.checkRecognitionSupport(
                    intent,
                    androidx.core.content.ContextCompat.getMainExecutor(context),
                    object : android.speech.RecognitionSupportCallback {
                        override fun onSupportResult(recognitionSupport: android.speech.RecognitionSupport) {
                            val supported = recognitionSupport.supportedOnDeviceLanguages
                            val result = defaultList.map { lang ->
                                lang.copy(isOfflineAvailable = supported.contains(lang.languageCode))
                            }
                            callback(result)
                            recognizer.destroy()
                        }
                        override fun onError(error: Int) {
                            callback(defaultList)
                            recognizer.destroy()
                        }
                    }
                )
            } catch (e: Exception) {
                callback(defaultList)
            }
        } else {
            callback(defaultList)
        }
    }

    data class LanguagePack(
        val displayName: String,
        val languageCode: String,
        val isOfflineAvailable: Boolean
    )
}
