package com.example.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TTSWrapper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    private val _currentRange = MutableStateFlow<Pair<Int, Int>?>(null)
    val currentRange: StateFlow<Pair<Int, Int>?> = _currentRange

    private var currentStartOffset = 0

    private var isInitializing = false
    private var pendingText: String? = null
    private var pendingStartOffset = 0
    private val appContext = context.applicationContext

    private var currentLanguageTag: String = "en-US"
    private var currentEnginePkg: String? = null

    init {
        // Initialize immediately to be ready
        initializeTTS()
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

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (utteranceId?.startsWith("TTS_ID_") == true) {
                        _currentRange.value = Pair(currentStartOffset + start, currentStartOffset + end)
                    }
                }
            })
            pendingText?.let {
                play(it, pendingStartOffset)
                pendingText = null
            }
        } else {
            isInitializing = false
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
            val locale = Locale.forLanguageTag(tag)
            tts?.language = locale
        } catch (e: Exception) {
            try {
                tts?.language = Locale.US
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
        return try {
            tts?.engines ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAvailableVoicesOrLocales(): List<Locale> {
        return try {
            if (isInitialized && tts != null) {
                tts?.availableLanguages?.toList() ?: listOf(Locale.US, Locale.UK, Locale.CANADA, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN, Locale.JAPANESE, Locale.CHINESE, Locale("es", "ES"), Locale("hi", "IN"))
            } else {
                listOf(Locale.US, Locale.UK, Locale.CANADA, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN, Locale.JAPANESE, Locale.CHINESE, Locale("es", "ES"), Locale("hi", "IN"))
            }
        } catch (e: Exception) {
            listOf(Locale.US, Locale.UK, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN, Locale.JAPANESE, Locale.CHINESE, Locale("es", "ES"))
        }
    }

    fun play(text: String, startOffset: Int = 0) {
        if (tts == null || !isInitialized) {
            pendingText = text
            pendingStartOffset = startOffset
            if (!isInitializing) {
                initializeTTS(currentEnginePkg)
            }
            return
        }
        val textToRead = text.substring(startOffset)
        if (textToRead.isBlank()) return
        
        currentStartOffset = startOffset
        tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID_${System.currentTimeMillis()}")
        _isPlaying.value = true
    }

    fun speakFeedback(text: String) {
        if (text.isBlank()) return
        if (tts == null || !isInitialized) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FEEDBACK_${System.currentTimeMillis()}")
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
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
        isInitializing = false
    }
}
