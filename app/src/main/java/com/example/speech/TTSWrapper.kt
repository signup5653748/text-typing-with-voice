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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                    _currentRange.value = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                    _currentRange.value = null
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    _currentRange.value = Pair(currentStartOffset + start, currentStartOffset + end)
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

    fun play(text: String, startOffset: Int = 0) {
        if (tts == null) {
            pendingText = text
            pendingStartOffset = startOffset
            if (!isInitializing) {
                isInitializing = true
                tts = TextToSpeech(appContext, this)
            }
            return
        }
        if (!isInitialized) return
        val textToRead = text.substring(startOffset)
        if (textToRead.isBlank()) return
        
        currentStartOffset = startOffset
        tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID_${System.currentTimeMillis()}")
        _isPlaying.value = true
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentRange.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
