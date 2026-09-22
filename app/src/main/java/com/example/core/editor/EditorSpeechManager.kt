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
 * and speech feedback. Ensures visual highlights ONLY show when TTS audio is actively playing.
 */
class EditorSpeechManager(
    val ttsWrapper: TTSWrapper,
    private val scope: CoroutineScope
) {
    private val _speechHighlightRange = MutableStateFlow<TextRange?>(null)
    val speechHighlightRange: StateFlow<TextRange?> = _speechHighlightRange.asStateFlow()

    private var speechPacingJob: Job? = null
    private var lastStoppedOffset: Int = 0
    private var currentPlaybackOffset: Int = 0

    init {
        scope.launch {
            ttsWrapper.isPlaying.collect { playing ->
                if (!playing) {
                    speechPacingJob?.cancel()
                    _speechHighlightRange.value = null
                }
            }
        }

        scope.launch {
            ttsWrapper.currentRange.collect { range ->
                if (ttsWrapper.isPlaying.value && range != null) {
                    currentPlaybackOffset = range.first
                    _speechHighlightRange.value = TextRange(range.first, range.second)
                } else if (!ttsWrapper.isPlaying.value) {
                    _speechHighlightRange.value = null
                }
            }
        }
    }

    fun stopPlayback() {
        if (_speechHighlightRange.value != null) {
            currentPlaybackOffset = _speechHighlightRange.value!!.start
        }
        lastStoppedOffset = currentPlaybackOffset
        if (ttsWrapper.isPlaying.value) {
            ttsWrapper.stop()
        }
        speechPacingJob?.cancel()
        _speechHighlightRange.value = null
    }

    fun playOrResume(
        docText: String,
        ttsSpeed: Float = 1.0f,
        highlightUnit: String = "LINE"
    ) {
        if (docText.isBlank()) {
            ttsWrapper.speakFeedback("Document is empty")
            return
        }
        val safeStart = lastStoppedOffset.coerceIn(0, docText.length)
        if (safeStart >= docText.length) {
            lastStoppedOffset = 0
            playFrom(docText, 0, docText.length, ttsSpeed, highlightUnit)
        } else {
            playFrom(docText, safeStart, docText.length, ttsSpeed, highlightUnit)
        }
    }

    fun restartPlayback(
        docText: String,
        ttsSpeed: Float = 1.0f,
        highlightUnit: String = "LINE"
    ) {
        if (docText.isBlank()) {
            ttsWrapper.speakFeedback("Document is empty")
            return
        }
        lastStoppedOffset = 0
        currentPlaybackOffset = 0
        playFrom(docText, 0, docText.length, ttsSpeed, highlightUnit)
    }

    fun resetPlaybackPosition() {
        lastStoppedOffset = 0
        currentPlaybackOffset = 0
    }

    fun playFrom(
        docText: String,
        startOffset: Int = 0,
        endOffset: Int? = null,
        ttsSpeed: Float = 1.0f,
        highlightUnit: String = "LINE"
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
                playFrom(docText, 0, docText.length, ttsSpeed, highlightUnit)
            }
            return
        }

        currentPlaybackOffset = safeStart
        lastStoppedOffset = safeStart

        val textToRead = docText.substring(safeStart, safeEnd)
        val sanitizedTextToRead = HeadingLogic.maskHeadingSymbolsForTTS(textToRead)
        if (sanitizedTextToRead.isBlank()) {
            return
        }

        val itemsToHighlight = mutableListOf<SpokenWord>()
        if (highlightUnit == "LINE") {
            var currIndex = 0
            val lines = sanitizedTextToRead.split('\n')
            for (line in lines) {
                val lineLen = line.length
                if (line.isNotBlank()) {
                    val lineStart = safeStart + currIndex
                    val lineEnd = safeStart + currIndex + lineLen
                    val wordCount = line.trim().split(Regex("\\s+")).count { it.isNotEmpty() }.coerceAtLeast(1)
                    val estDuration = (wordCount * 280L) + 200L
                    itemsToHighlight.add(SpokenWord(TextRange(lineStart, lineEnd), line, estDuration))
                }
                currIndex += lineLen + 1
            }
        } else {
            val wordRegex = Regex("\\b[\\p{L}\\p{N}']+\\b|\\S+")
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
                    itemsToHighlight.add(SpokenWord(TextRange(wordStart, wordEnd), token, pauseAfter))
                }
            }
        }

        ttsWrapper.play(sanitizedTextToRead, safeStart)

        speechPacingJob?.cancel()
        speechPacingJob = scope.launch {
            var waited = 0
            while (!ttsWrapper.isPlaying.value && waited < 30) {
                delay(50)
                waited++
            }

            // If TTS did not start playing or audio failed, do NOT highlight anything!
            if (!ttsWrapper.isPlaying.value) {
                _speechHighlightRange.value = null
                return@launch
            }

            val currentSpeed = ttsSpeed.coerceIn(0.5f, 3.0f)
            var nativeRangeReported = false

            val nativeCollectorJob = launch {
                ttsWrapper.currentRange.collect { range ->
                    if (range != null && ttsWrapper.isPlaying.value) {
                        nativeRangeReported = true
                        if (highlightUnit == "LINE") {
                            val s = range.first.coerceIn(0, docText.length)
                            val prevNl = docText.lastIndexOf('\n', (s - 1).coerceAtLeast(0))
                            val lStart = if (prevNl == -1) 0 else prevNl + 1
                            val nextNl = docText.indexOf('\n', s)
                            val lEnd = if (nextNl == -1) docText.length else nextNl
                            _speechHighlightRange.value = TextRange(lStart, maxOf(lStart, lEnd))
                        } else {
                            _speechHighlightRange.value = TextRange(range.first, range.second)
                        }
                    }
                }
            }

            try {
                for (item in itemsToHighlight) {
                    if (!ttsWrapper.isPlaying.value) break

                    if (nativeRangeReported) {
                        while (ttsWrapper.isPlaying.value && nativeRangeReported) {
                            delay(100L)
                        }
                        break
                    }

                    if (!ttsWrapper.isPlaying.value) break
                    _speechHighlightRange.value = item.docRange
                    currentPlaybackOffset = item.docRange.start

                    val duration = if (highlightUnit == "LINE") {
                        (item.pauseAfterMs / currentSpeed).toLong().coerceAtLeast(200L)
                    } else {
                        val length = item.word.length
                        val baseDuration = when {
                            length <= 3 -> 200L
                            length <= 6 -> 300L
                            length <= 9 -> 400L
                            else -> 500L
                        }
                        ((baseDuration + item.pauseAfterMs) / currentSpeed).toLong().coerceAtLeast(80L)
                    }

                    val stepMs = 60L
                    val steps = (duration / stepMs).coerceAtLeast(1L)
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
