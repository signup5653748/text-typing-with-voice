package com.example.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.logic.HeadingLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class TtsLanguageItem(
    val languageTag: String,
    val displayName: String,
    val locale: Locale,
    val isDownloaded: Boolean = true,
    val voiceCount: Int = 1
)

class TTSWrapper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    private val _currentRange = MutableStateFlow<Pair<Int, Int>?>(null)
    val currentRange: StateFlow<Pair<Int, Int>?> = _currentRange

    private val _availableTtsLanguages = MutableStateFlow<List<TtsLanguageItem>>(emptyList())
    val availableTtsLanguages: StateFlow<List<TtsLanguageItem>> = _availableTtsLanguages.asStateFlow()

    private var currentStartOffset = 0

    private var isInitializing = false
    private var pendingText: String? = null
    private var pendingStartOffset = 0
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Default)

    private var currentLanguageTag: String = "en-US"
    private var currentEnginePkg: String? = null
    private var currentRate: Float = 1.0f
    private var currentPitch: Float = 1.0f

    init {
        // Eagerly initialize TTS engine so it is connected and ready for instant playback
        initializeTTS()
    }

    fun ensureInitialized(enginePkg: String? = null) {
        if (tts == null && !isInitializing && !isInitialized) {
            initializeTTS(enginePkg ?: currentEnginePkg)
        }
    }

    private fun initializeTTS(enginePkg: String? = null) {
        try {
            isInitializing = true
            currentEnginePkg = enginePkg
            if (enginePkg.isNullOrBlank()) {
                tts = TextToSpeech(appContext, this)
            } else {
                tts = TextToSpeech(appContext, this, enginePkg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isInitializing = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            isInitializing = false
            applyLanguage(currentLanguageTag)
            tts?.setSpeechRate(currentRate)
            tts?.setPitch(currentPitch)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId?.startsWith("TTS_ID_") == true) {
                        _isPlaying.value = true
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId?.startsWith("TTS_ID_") == true) {
                        _isPlaying.value = false
                        _currentRange.value = null
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId?.startsWith("TTS_ID_") == true) {
                        _isPlaying.value = false
                        _currentRange.value = null
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (utteranceId?.startsWith("TTS_ID_") == true) {
                        _isPlaying.value = false
                        _currentRange.value = null
                    }
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (utteranceId?.startsWith("TTS_ID_") == true && _isPlaying.value) {
                        _currentRange.value = Pair(currentStartOffset + start, currentStartOffset + end)
                    }
                }
            })

            // Refresh available / downloaded TTS languages
            refreshAvailableLanguages()

            pendingText?.let {
                val text = it
                val offset = pendingStartOffset
                pendingText = null
                play(text, offset)
            }
        } else {
            isInitialized = false
            isInitializing = false
            _isPlaying.value = false
            _currentRange.value = null
        }
    }

    fun refreshAvailableLanguages() {
        scope.launch {
            val list = queryDownloadedTtsLanguages()
            _availableTtsLanguages.value = list
        }
    }

    fun queryDownloadedTtsLanguages(): List<TtsLanguageItem> {
        val ttsInstance = tts ?: return emptyList()
        val result = linkedMapOf<String, TtsLanguageItem>()
        val defaultLoc = Locale.getDefault()

        // Strategy 1: Check voices (modern Android API 21+)
        try {
            val voices: Set<Voice>? = ttsInstance.voices
            if (!voices.isNullOrEmpty()) {
                for (voice in voices) {
                    val loc = voice.locale ?: continue
                    val tag = loc.toLanguageTag().ifBlank { "${loc.language}-${loc.country}".trimEnd('-') }
                    val isNotInstalled = voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true
                    val isNetwork = voice.isNetworkConnectionRequired
                    val isDownloaded = !isNotInstalled && !isNetwork

                    val existing = result[tag]
                    if (existing == null) {
                        val label = loc.getDisplayName(defaultLoc).ifBlank { loc.displayName }.replaceFirstChar { it.uppercase() }
                        result[tag] = TtsLanguageItem(
                            languageTag = tag,
                            displayName = label,
                            locale = loc,
                            isDownloaded = isDownloaded,
                            voiceCount = 1
                        )
                    } else {
                        result[tag] = existing.copy(
                            isDownloaded = existing.isDownloaded || isDownloaded,
                            voiceCount = existing.voiceCount + 1
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Strategy 2: Check availableLanguages
        try {
            val availableLangs = ttsInstance.availableLanguages
            if (!availableLangs.isNullOrEmpty()) {
                for (loc in availableLangs) {
                    val tag = loc.toLanguageTag().ifBlank { "${loc.language}-${loc.country}".trimEnd('-') }
                    val availability = try { ttsInstance.isLanguageAvailable(loc) } catch (_: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
                    val isDownloaded = availability >= TextToSpeech.LANG_AVAILABLE

                    val existing = result[tag]
                    if (existing == null) {
                        val label = loc.getDisplayName(defaultLoc).ifBlank { loc.displayName }.replaceFirstChar { it.uppercase() }
                        result[tag] = TtsLanguageItem(
                            languageTag = tag,
                            displayName = label,
                            locale = loc,
                            isDownloaded = isDownloaded,
                            voiceCount = 1
                        )
                    } else if (isDownloaded) {
                        result[tag] = existing.copy(isDownloaded = true)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Strategy 3: Check standard and device installed locales if nothing found
        if (result.isEmpty()) {
            val candidateLocales = mutableListOf<Locale>().apply {
                add(Locale.getDefault())
                addAll(Locale.getAvailableLocales())
            }.distinctBy { it.language to it.country }

            for (loc in candidateLocales) {
                if (loc.language.isBlank()) continue
                val availability = try { ttsInstance.isLanguageAvailable(loc) } catch (_: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
                if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    val tag = loc.toLanguageTag().ifBlank { "${loc.language}-${loc.country}".trimEnd('-') }
                    val label = loc.getDisplayName(defaultLoc).ifBlank { loc.displayName }.replaceFirstChar { it.uppercase() }
                    result[tag] = TtsLanguageItem(
                        languageTag = tag,
                        displayName = label,
                        locale = loc,
                        isDownloaded = true,
                        voiceCount = 1
                    )
                }
            }
        }

        return if (result.isNotEmpty()) {
            result.values.sortedWith(
                compareByDescending<TtsLanguageItem> { it.isDownloaded }
                    .thenBy { it.displayName }
            )
        } else {
            listOf(
                TtsLanguageItem("en-US", "English (United States)", Locale.US, true),
                TtsLanguageItem("en-GB", "English (United Kingdom)", Locale.UK, true),
                TtsLanguageItem("es-ES", "Spanish (Spain)", Locale("es", "ES"), true),
                TtsLanguageItem("fr-FR", "French (France)", Locale.FRANCE, true),
                TtsLanguageItem("de-DE", "German (Germany)", Locale.GERMANY, true),
                TtsLanguageItem("it-IT", "Italian (Italy)", Locale.ITALY, true),
                TtsLanguageItem("ja-JP", "Japanese (Japan)", Locale.JAPAN, true),
                TtsLanguageItem("hi-IN", "Hindi (India)", Locale("hi", "IN"), true)
            )
        }
    }

    fun setSpeechRate(rate: Float) {
        currentRate = rate
        if (isInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch
        if (isInitialized) {
            tts?.setPitch(pitch)
        }
    }

    fun setLanguage(languageTag: String) {
        currentLanguageTag = languageTag
        if (isInitialized) {
            applyLanguage(languageTag)
        }
    }

    private fun applyLanguage(tag: String) {
        try {
            val targetLocale = if (tag.isNotBlank()) Locale.forLanguageTag(tag) else Locale.getDefault()
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val langOnly = Locale(targetLocale.language)
                val fallbackResult = tts?.setLanguage(langOnly)
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val defaultResult = tts?.setLanguage(Locale.getDefault())
                    if (defaultResult == TextToSpeech.LANG_MISSING_DATA || defaultResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                }
            }
        } catch (e: Exception) {
            try {
                tts?.setLanguage(Locale.US)
            } catch (_: Exception) {}
        }
    }

    fun setEngine(enginePkg: String) {
        if (currentEnginePkg != enginePkg) {
            shutdown()
            initializeTTS(enginePkg.ifBlank { null })
        }
    }

    fun getAvailableEngines(): List<TextToSpeech.EngineInfo> {
        ensureInitialized()
        return try {
            tts?.engines ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAvailableVoicesOrLocales(): List<Locale> {
        ensureInitialized()
        val downloaded = _availableTtsLanguages.value.ifEmpty { queryDownloadedTtsLanguages() }
        return downloaded.map { it.locale }
    }

    fun play(textToRead: String, documentStartOffset: Int = 0) {
        val sanitized = HeadingLogic.maskHeadingSymbolsForTTS(textToRead)
        if (sanitized.isBlank()) {
            stop()
            return
        }

        if (tts == null || !isInitialized) {
            pendingText = sanitized
            pendingStartOffset = documentStartOffset
            if (!isInitializing) {
                initializeTTS(currentEnginePkg)
            }
            return
        }
        
        currentStartOffset = documentStartOffset
        val utteranceId = "TTS_ID_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = tts?.speak(sanitized, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            _isPlaying.value = false
            _currentRange.value = null
        }
    }

    fun speakFeedback(text: String) {
        val sanitized = HeadingLogic.stripHeadingSymbolsForTTS(text)
        if (sanitized.isBlank()) return
        if (tts == null || !isInitialized) {
            ensureInitialized()
            return
        }
        try {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(sanitized, TextToSpeech.QUEUE_FLUSH, params, "FEEDBACK_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isPlaying.value = false
        _currentRange.value = null
    }

    fun shutdown() {
        try {
            tts?.setOnUtteranceProgressListener(null)
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
        isInitializing = false
        pendingText = null
        _isPlaying.value = false
        _currentRange.value = null
    }
}
