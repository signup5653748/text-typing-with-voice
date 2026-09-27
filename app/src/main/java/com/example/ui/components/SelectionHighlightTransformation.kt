package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.logic.HeadingLogic
import kotlin.math.max
import kotlin.math.min

class SelectionHighlightTransformation(
    private val selection: TextRange,
    private val transientHighlight: TextRange? = null,
    private val speechHighlight: TextRange? = null,
    private val highlightColor: Color = Color(0xFF38BDF8).copy(alpha = 0.45f),
    private val speechHighlightColor: Color = Color(0xFF38BDF8),
    private val highlightedTextColor: Color = Color.White,
    private val hideHeadingSymbols: Boolean = true,
    private val cachedMapping: CachedOffsetMap? = null,
    private val highlightOverlayEnabled: Boolean = true
) : VisualTransformation {

    data class CachedOffsetMap(
        val origToTrans: IntArray,
        val transToOrig: IntArray,
        val transformedTextStr: String
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val origText = text.text

        // Case 1: Raw symbols visible (no hide-on-render transform)
        if (!hideHeadingSymbols || origText.isEmpty()) {
            val effectiveRange = getEffectiveRange()
            val speechRange = getSpeechRange(origText.length)

            if (effectiveRange == null && speechRange == null) {
                return TransformedText(text, OffsetMapping.Identity)
            }

            val annotated = buildAnnotatedString {
                append(origText)
                if (effectiveRange != null) {
                    val start = min(effectiveRange.start, effectiveRange.end).coerceIn(0, origText.length)
                    val end = max(effectiveRange.start, effectiveRange.end).coerceIn(0, origText.length)
                    if (start < end) {
                        addStyle(
                            SpanStyle(
                                background = if (speechRange != null) highlightColor.copy(alpha = 0.35f) else highlightColor,
                                color = highlightedTextColor
                            ),
                            start,
                            end
                        )
                    }
                }
                if (speechRange != null) {
                    val sStart = min(speechRange.start, speechRange.end).coerceIn(0, origText.length)
                    val sEnd = max(speechRange.start, speechRange.end).coerceIn(0, origText.length)
                    if (sStart < sEnd) {
                        addStyle(
                            SpanStyle(
                                background = speechHighlightColor,
                                color = Color(0xFF090D16)
                            ),
                            sStart,
                            sEnd
                        )
                    }
                }
            }
            return TransformedText(annotated, OffsetMapping.Identity)
        }

        // Case 2: Hide-on-render active - transform leading heading symbols
        val origLen = origText.length
        val mapping = cachedMapping ?: computeOffsetMap(origText)
        val origToTrans = mapping.origToTrans
        val transToOrig = mapping.transToOrig
        val transformedTextStr = mapping.transformedTextStr

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (origToTrans.isEmpty()) return 0
                val clamped = offset.coerceIn(0, origToTrans.lastIndex)
                return origToTrans[clamped].coerceIn(0, transformedTextStr.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (transToOrig.isEmpty()) return 0
                val clamped = offset.coerceIn(0, transToOrig.lastIndex)
                return transToOrig[clamped].coerceIn(0, origLen)
            }
        }

        val effectiveRange = getEffectiveRange()
        val speechRange = getSpeechRange(origLen)

        if (effectiveRange == null && speechRange == null) {
            return TransformedText(AnnotatedString(transformedTextStr), offsetMapping)
        }

        val annotated = buildAnnotatedString {
            append(transformedTextStr)
            if (effectiveRange != null) {
                val origStart = min(effectiveRange.start, effectiveRange.end).coerceIn(0, origLen)
                val origEnd = max(effectiveRange.start, effectiveRange.end).coerceIn(0, origLen)
                val transStart = offsetMapping.originalToTransformed(origStart).coerceIn(0, transformedTextStr.length)
                val transEnd = offsetMapping.originalToTransformed(origEnd).coerceIn(0, transformedTextStr.length)
                if (transStart < transEnd) {
                    addStyle(
                        SpanStyle(
                            background = if (speechRange != null) highlightColor.copy(alpha = 0.35f) else highlightColor,
                            color = highlightedTextColor
                        ),
                        transStart,
                        transEnd
                    )
                }
            }
            if (speechRange != null) {
                val origStart = min(speechRange.start, speechRange.end).coerceIn(0, origLen)
                val origEnd = max(speechRange.start, speechRange.end).coerceIn(0, origLen)
                val transStart = offsetMapping.originalToTransformed(origStart).coerceIn(0, transformedTextStr.length)
                val transEnd = offsetMapping.originalToTransformed(origEnd).coerceIn(0, transformedTextStr.length)
                if (transStart < transEnd) {
                    addStyle(
                        SpanStyle(
                            background = speechHighlightColor,
                            color = Color(0xFF090D16)
                        ),
                        transStart,
                        transEnd
                    )
                }
            }
        }

        return TransformedText(annotated, offsetMapping)
    }

    private fun getSpeechRange(maxLen: Int): TextRange? {
        val range = speechHighlight ?: return null
        val start = min(range.start, range.end).coerceIn(0, maxLen)
        val end = max(range.start, range.end).coerceIn(0, maxLen)
        return if (start < end) TextRange(start, end) else null
    }

    private fun getEffectiveRange(): TextRange? {
        if (!highlightOverlayEnabled) return null
        return if (selection.length > 0) {
            selection
        } else if (transientHighlight != null && transientHighlight.length > 0) {
            transientHighlight
        } else {
            null
        }
    }

    companion object {
        @Volatile
        private var lastComputedMap: Pair<String, CachedOffsetMap>? = null

        fun computeOffsetMap(origText: String): CachedOffsetMap {
            val cached = lastComputedMap
            if (cached != null && cached.first == origText) {
                return cached.second
            }

            val origLen = origText.length
            if (origLen == 0) {
                val emptyMap = CachedOffsetMap(IntArray(1), IntArray(1), "")
                lastComputedMap = Pair("", emptyMap)
                return emptyMap
            }

            val origToTrans = IntArray(origLen + 1)
            val transToOrig = IntArray(origLen + 1)
            val transBuilder = StringBuilder(origLen)

            var origIdx = 0
            var transIdx = 0

            while (origIdx < origLen) {
                var lineEndIdx = origText.indexOf('\n', startIndex = origIdx)
                if (lineEndIdx < 0) lineEndIdx = origLen

                val lineLength = lineEndIdx - origIdx
                val (_, symbolLen) = HeadingLogic.getHeadingLevelAndLength(origText, origIdx, lineEndIdx)
                val safeSymbolLen = min(symbolLen, lineLength)

                for (i in 0 until safeSymbolLen) {
                    origToTrans[origIdx + i] = transIdx
                }

                for (i in safeSymbolLen until lineLength) {
                    val currentOrig = origIdx + i
                    origToTrans[currentOrig] = transIdx
                    transToOrig[transIdx] = currentOrig
                    transBuilder.append(origText[currentOrig])
                    transIdx++
                }

                if (lineEndIdx < origLen) {
                    origToTrans[lineEndIdx] = transIdx
                    transToOrig[transIdx] = lineEndIdx
                    transBuilder.append('\n')
                    transIdx++
                    origIdx = lineEndIdx + 1
                } else {
                    origIdx = lineEndIdx
                }
            }

            origToTrans[origLen] = transIdx
            transToOrig[transIdx] = origLen
            transIdx++

            val finalTransToOrig = if (transIdx == transToOrig.size) transToOrig else transToOrig.copyOf(transIdx)

            val computed = CachedOffsetMap(
                origToTrans = origToTrans,
                transToOrig = finalTransToOrig,
                transformedTextStr = transBuilder.toString()
            )
            lastComputedMap = Pair(origText, computed)
            return computed
        }

        fun originalToTransformed(origOffset: Int, mapping: CachedOffsetMap?): Int {
            if (mapping == null) return origOffset
            if (mapping.origToTrans.isEmpty()) return 0
            val clamped = origOffset.coerceIn(0, mapping.origToTrans.lastIndex)
            return mapping.origToTrans[clamped].coerceIn(0, mapping.transformedTextStr.length)
        }

        fun transformedToOriginal(transOffset: Int, mapping: CachedOffsetMap?): Int {
            if (mapping == null) return transOffset
            if (mapping.transToOrig.isEmpty()) return 0
            val clamped = transOffset.coerceIn(0, mapping.transToOrig.lastIndex)
            return mapping.transToOrig[clamped]
        }

        fun originalToTransformed(origText: String, origOffset: Int, hideHeadingSymbols: Boolean): Int {
            if (!hideHeadingSymbols || origText.isEmpty()) {
                return origOffset.coerceIn(0, origText.length)
            }
            val mapping = computeOffsetMap(origText)
            return originalToTransformed(origOffset, mapping)
        }

        fun transformedToOriginal(origText: String, transOffset: Int, hideHeadingSymbols: Boolean): Int {
            if (!hideHeadingSymbols || origText.isEmpty()) {
                return transOffset.coerceIn(0, origText.length)
            }
            val mapping = computeOffsetMap(origText)
            return transformedToOriginal(transOffset, mapping)
        }
    }
}


