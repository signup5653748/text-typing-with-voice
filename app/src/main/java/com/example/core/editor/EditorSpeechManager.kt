package com.example.core.editor

import androidx.compose.ui.text.TextRange
import com.example.logic.HeadingLogic
import com.example.speech.TTSWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpokenWord(
    val docRange: TextRange,
    val word: String,
    val pauseAfterMs: Long
)

/**
 * Coordinates read-aloud TTS playback, word-by-word visual highlighting pacing,
 * and speech feedback.
 */
class EditorSpeechManager(
    val ttsWrapper: TTSWrapper,
    private val scope: CoroutineScope
) {
    private val _speechHighlightRange = MutableStateFlow<TextRange?>(null)
    val speechHighlightRange: StateFlow<TextRange?> = _speechHighlightRange.asStateFlow()

    private var speechPacingJob: Job? = null

    init {
        scope.launch {
            ttsWrapper.isPlaying.collect { playing ->
                if (!playing) {
                    speechPacingJob?.cancel()
                    _speechHighlightRange.value = null
                }
            }
        }
    }

    fun stopPlayback() {
        if (ttsWrapper.isPlaying.value) {
            ttsWrapper.stop()
        }
        speechPacingJob?.cancel()
        _speechHighlightRange.value = null
    }

    fun playFrom(
        docText: String,
        startOffset: Int = 0,
        endOffset: Int? = null,
        ttsSpeed: Float = 1.0f
    ) {
        stopPlayback()
        if (docText.isBlank()) {
            ttsWrapper.speakFeedback("Document is empty")
            return
        }

        val safeStart = startOffset.coerceIn(0, docText.length)
        val safeEnd = (endOffset ?: docText.length).coerceIn(safeStart, docText.length)
        if (safeStart >= safeEnd) {
            if (safeStart >= docText.length && docText.isNotEmpty()) {
                playFrom(docText, 0, docText.length, ttsSpeed)
            }
            return
        }

        val textToRead = docText.substring(safeStart, safeEnd)
        val sanitizedTextToRead = HeadingLogic.maskHeadingSymbolsForTTS(textToRead)

        val wordRegex = Regex("\\b[\\p{L}\\p{N}']+\\b|\\S+")
        val words = mutableListOf<SpokenWord>()
        for (match in wordRegex.findAll(sanitizedTextToRead)) {
            val token = match.value
            if (token.any { it.isLetterOrDigit() }) {
                val wordStart = safeStart + match.range.first
                val wordEnd = safeStart + match.range.last + 1
                val nextIdx = match.range.last + 1
                val pauseAfter = if (nextIdx < sanitizedTextToRead.length) {
                    when (sanitizedTextToRead[nextIdx]) {
                        '.', '!', '?' -> 260L
                        ',', ';', ':', '—', '-' -> 160L
                        '\n' -> 220L
                        else -> 0L
                    }
                } else 0L
                words.add(SpokenWord(TextRange(wordStart, wordEnd), token, pauseAfter))
            }
        }

        ttsWrapper.play(sanitizedTextToRead, safeStart)

        speechPacingJob?.cancel()
        speechPacingJob = scope.launch {
            var waited = 0
            while (!ttsWrapper.isPlaying.value && waited < 20) {
                delay(50)
                waited++
            }
            if (!ttsWrapper.isPlaying.value && words.isEmpty()) {
                _speechHighlightRange.value = null
                return@launch
            }

            val currentSpeed = ttsSpeed.coerceIn(0.5f, 3.0f)
            var nativeRangeReported = false

            val nativeCollectorJob = launch {
                ttsWrapper.currentRange.collect { range ->
                    if (range != null) {
                        nativeRangeReported = true
                        _speechHighlightRange.value = TextRange(range.first, range.second)
                    }
                }
            }

            try {
                for (item in words) {
                    if (!ttsWrapper.isPlaying.value) break

                    if (nativeRangeReported) {
                        while (ttsWrapper.isPlaying.value && nativeRangeReported) {
                            delay(100L)
                        }
                        break
                    }

                    _speechHighlightRange.value = item.docRange

                    val length = item.word.length
                    val baseDuration = when {
                        length <= 3 -> 200L
                        length <= 6 -> 300L
                        length <= 9 -> 400L
                        else -> 500L
                    }
                    val wordDuration = ((baseDuration + item.pauseAfterMs) / currentSpeed)
                        .toLong()
                        .coerceAtLeast(80L)

                    val stepMs = 75L
                    val steps = (wordDuration / stepMs).coerceAtLeast(1L)
                    for (s in 0 until steps) {
                        if (!ttsWrapper.isPlaying.value || nativeRangeReported) break
                        delay(stepMs)
                    }
                }

                while (ttsWrapper.isPlaying.value) {
                    delay(100L)
                }
            } finally {
                nativeCollectorJob.cancel()
                _speechHighlightRange.value = null
            }
        }
    }

    fun speakFeedback(phrase: String) {
        ttsWrapper.speakFeedback(phrase)
    }

    fun shutdown() {
        speechPacingJob?.cancel()
        ttsWrapper.shutdown()
    }
}
