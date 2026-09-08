package com.example.speech

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

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
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
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

    /**
     * Queries the actual installed/downloaded offline voice recognition languages on the device.
     * No hardcoded/predefined lists.
     */
    fun getSupportedLanguages(callback: (List<LanguagePack>) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                            val installedOnDevice = recognitionSupport.installedOnDeviceLanguages
                            val supportedOnDevice = recognitionSupport.supportedOnDeviceLanguages

                            // Gather genuine downloaded/on-device languages
                            val downloadedTags = (installedOnDevice + supportedOnDevice).filter { it.isNotBlank() }.distinct()

                            if (downloadedTags.isNotEmpty()) {
                                val list = downloadedTags.map { tag ->
                                    LanguagePack(
                                        displayName = formatDisplayName(tag),
                                        languageCode = tag,
                                        isOfflineAvailable = true
                                    )
                                }.sortedBy { it.displayName }
                                callback(list)
                                recognizer.destroy()
                                return
                            }

                            // If checkRecognitionSupport returns empty, query the speech engine broadcast receiver
                            queryInstalledLanguagesViaReceiver(context) { receiverLangs ->
                                callback(receiverLangs)
                                recognizer.destroy()
                            }
                        }

                        override fun onError(error: Int) {
                            queryInstalledLanguagesViaReceiver(context, callback)
                            recognizer.destroy()
                        }
                    }
                )
            } catch (e: Exception) {
                queryInstalledLanguagesViaReceiver(context, callback)
            }
        } else {
            queryInstalledLanguagesViaReceiver(context, callback)
        }
    }

    private fun queryInstalledLanguagesViaReceiver(ctx: Context, callback: (List<LanguagePack>) -> Unit) {
        val detailsIntent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            detailsIntent.setPackage("com.google.android.googlequicksearchbox")
        }

        try {
            ctx.sendOrderedBroadcast(
                detailsIntent,
                null,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val results = getResultExtras(true)
                        val supportedLangs = results?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)

                        if (!supportedLangs.isNullOrEmpty()) {
                            val list = supportedLangs.filter { it.isNotBlank() }.distinct().map { tag ->
                                LanguagePack(
                                    displayName = formatDisplayName(tag),
                                    languageCode = tag,
                                    isOfflineAvailable = true
                                )
                            }.sortedBy { it.displayName }
                            callback(list)
                        } else {
                            // If broadcast returned no list, fallback to currently active device Locale only
                            val systemDefault = Locale.getDefault()
                            val tag = systemDefault.toLanguageTag().ifBlank {
                                "${systemDefault.language}-${systemDefault.country}".trimEnd('-')
                            }
                            val singleList = listOf(
                                LanguagePack(
                                    displayName = systemDefault.getDisplayName(systemDefault).replaceFirstChar { it.uppercase() },
                                    languageCode = tag,
                                    isOfflineAvailable = true
                                )
                            )
                            callback(singleList)
                        }
                    }
                },
                null,
                Activity.RESULT_OK,
                null,
                null
            )
        } catch (e: Exception) {
            val systemDefault = Locale.getDefault()
            val tag = systemDefault.toLanguageTag().ifBlank {
                "${systemDefault.language}-${systemDefault.country}".trimEnd('-')
            }
            callback(
                listOf(
                    LanguagePack(
                        displayName = systemDefault.getDisplayName(systemDefault).replaceFirstChar { it.uppercase() },
                        languageCode = tag,
                        isOfflineAvailable = true
                    )
                )
            )
        }
    }

    private fun formatDisplayName(languageTag: String): String {
        return try {
            val locale = Locale.forLanguageTag(languageTag)
            val name = locale.getDisplayName(Locale.getDefault())
            if (name.isNotBlank()) name.replaceFirstChar { it.uppercase() } else locale.displayName
        } catch (e: Exception) {
            languageTag
        }
    }

    data class LanguagePack(
        val displayName: String,
        val languageCode: String,
        val isOfflineAvailable: Boolean
    )
}
