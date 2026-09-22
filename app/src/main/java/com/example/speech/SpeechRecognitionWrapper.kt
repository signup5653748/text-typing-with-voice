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

    companion object {
        val ALL_SPEECH_LANGUAGES = listOf(
            "en-US" to "English (United States)",
            "en-GB" to "English (United Kingdom)",
            "en-IN" to "English (India)",
            "en-AU" to "English (Australia)",
            "en-CA" to "English (Canada)",
            "en-NZ" to "English (New Zealand)",
            "en-ZA" to "English (South Africa)",
            "en-IE" to "English (Ireland)",
            "en-SG" to "English (Singapore)",
            "hi-IN" to "Hindi (India)",
            "es-ES" to "Spanish (Spain)",
            "es-US" to "Spanish (United States)",
            "es-MX" to "Spanish (Mexico)",
            "es-AR" to "Spanish (Argentina)",
            "es-CO" to "Spanish (Colombia)",
            "fr-FR" to "French (France)",
            "fr-CA" to "French (Canada)",
            "fr-BE" to "French (Belgium)",
            "de-DE" to "German (Germany)",
            "de-AT" to "German (Austria)",
            "de-CH" to "German (Switzerland)",
            "it-IT" to "Italian (Italy)",
            "pt-BR" to "Portuguese (Brazil)",
            "pt-PT" to "Portuguese (Portugal)",
            "ru-RU" to "Russian (Russia)",
            "ja-JP" to "Japanese (Japan)",
            "ko-KR" to "Korean (South Korea)",
            "zh-CN" to "Chinese (Simplified, China)",
            "zh-TW" to "Chinese (Traditional, Taiwan)",
            "zh-HK" to "Chinese (Cantonese, Hong Kong)",
            "ar-SA" to "Arabic (Saudi Arabia)",
            "ar-AE" to "Arabic (UAE)",
            "ar-EG" to "Arabic (Egypt)",
            "tr-TR" to "Turkish (Turkey)",
            "nl-NL" to "Dutch (Netherlands)",
            "pl-PL" to "Polish (Poland)",
            "sv-SE" to "Swedish (Sweden)",
            "id-ID" to "Indonesian (Indonesia)",
            "th-TH" to "Thai (Thailand)",
            "vi-VN" to "Vietnamese (Vietnam)",
            "bn-IN" to "Bengali (India)",
            "ta-IN" to "Tamil (India)",
            "te-IN" to "Telugu (India)",
            "mr-IN" to "Marathi (India)",
            "gu-IN" to "Gujarati (India)",
            "kn-IN" to "Kannada (India)",
            "ml-IN" to "Malayalam (India)",
            "pa-IN" to "Punjabi (India)",
            "ur-IN" to "Urdu (India)",
            "ur-PK" to "Urdu (Pakistan)",
            "uk-UA" to "Ukrainian (Ukraine)",
            "el-GR" to "Greek (Greece)",
            "cs-CZ" to "Czech (Czech Republic)",
            "ro-RO" to "Romanian (Romania)",
            "hu-HU" to "Hungarian (Hungary)",
            "he-IL" to "Hebrew (Israel)",
            "fa-IR" to "Persian (Iran)",
            "ms-MY" to "Malay (Malaysia)",
            "fil-PH" to "Filipino (Philippines)"
        )
    }

    /**
     * Queries both installed/downloaded offline voice recognition languages on the device
     * and full supported world language list so user can use any language.
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
                            val downloadedSet = (installedOnDevice + supportedOnDevice).filter { it.isNotBlank() }.toSet()

                            val combinedList = buildCompleteLanguageList(downloadedSet)
                            callback(combinedList)
                            recognizer.destroy()
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
                        val supportedLangs = results?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES) ?: emptyList<String>()
                        val downloadedSet = supportedLangs.filter { it.isNotBlank() }.toSet()
                        val list = buildCompleteLanguageList(downloadedSet)
                        callback(list)
                    }
                },
                null,
                Activity.RESULT_OK,
                null,
                null
            )
        } catch (e: Exception) {
            callback(buildCompleteLanguageList(emptySet()))
        }
    }

    private fun buildCompleteLanguageList(downloadedTags: Set<String>): List<LanguagePack> {
        val result = linkedMapOf<String, LanguagePack>()

        // 1. First add downloaded/on-device tags
        downloadedTags.forEach { tag ->
            result[tag] = LanguagePack(
                displayName = formatDisplayName(tag),
                languageCode = tag,
                isOfflineAvailable = true
            )
        }

        // 2. Add full catalog of world languages
        ALL_SPEECH_LANGUAGES.forEach { (tag, label) ->
            if (!result.containsKey(tag)) {
                val isDownloaded = downloadedTags.contains(tag) || downloadedTags.any { it.startsWith(tag, ignoreCase = true) }
                result[tag] = LanguagePack(
                    displayName = label,
                    languageCode = tag,
                    isOfflineAvailable = isDownloaded
                )
            }
        }

        return result.values.sortedWith(
            compareByDescending<LanguagePack> { it.isOfflineAvailable }
                .thenBy { it.displayName }
        )
    }

    fun openVoiceDownloadSettings(ctx: Context): Boolean {
        val intents = listOf(
            Intent("com.google.android.voicesearch.intent.action.DOWNLOAD_VOICE_PACK"),
            Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent("android.speech.action.VOICE_SETTINGS"),
            Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS),
            Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }
        return false
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
