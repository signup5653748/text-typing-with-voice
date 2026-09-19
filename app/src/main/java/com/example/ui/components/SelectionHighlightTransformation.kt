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
    private val highlightColor: Color = Color(0xFFFFD600).copy(alpha = 0.55f),
    private val speechHighlightColor: Color = Color(0xFFFFD600),
    private val highlightedTextColor: Color = Color.White,
    private val hideHeadingSymbols: Boolean = true,
    private val cachedMapping: CachedOffsetMap? = null
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
        return if (selection.length > 0) {
            selection
        } else if (transientHighlight != null && transientHighlight.length > 0) {
            transientHighlight
        } else {
            null
        }
    }

    companion object {
        fun computeOffsetMap(origText: String): CachedOffsetMap {
            val origLen = origText.length
            val origToTrans = IntArray(origLen + 1)
            val transToOrigList = mutableListOf<Int>()
            val transBuilder = StringBuilder()

            var origIdx = 0
            while (origIdx < origLen) {
                val lineEndIdx = origText.indexOf('\n', startIndex = origIdx).let { if (it >= 0) it else origLen }
                val lineLength = lineEndIdx - origIdx
                val lineContent = origText.substring(origIdx, lineEndIdx)
                val (_, symbolLen) = HeadingLogic.getHeadingLevelAndLength(lineContent)

                for (i in 0 until min(symbolLen, lineLength)) {
                    origToTrans[origIdx + i] = transBuilder.length
                }

                for (i in symbolLen until lineLength) {
                    val currentOrig = origIdx + i
                    origToTrans[currentOrig] = transBuilder.length
                    transToOrigList.add(currentOrig)
                    transBuilder.append(origText[currentOrig])
                }

                if (lineEndIdx < origLen) {
                    origToTrans[lineEndIdx] = transBuilder.length
                    transToOrigList.add(lineEndIdx)
                    transBuilder.append('\n')
                    origIdx = lineEndIdx + 1
                } else {
                    origIdx = lineEndIdx
                }
            }

            origToTrans[origLen] = transBuilder.length
            transToOrigList.add(origLen)
            return CachedOffsetMap(
                origToTrans = origToTrans,
                transToOrig = transToOrigList.toIntArray(),
                transformedTextStr = transBuilder.toString()
            )
        }

        fun transformedToOriginal(origText: String, transOffset: Int, hideHeadingSymbols: Boolean): Int {
            if (!hideHeadingSymbols || origText.isEmpty()) {
                return transOffset.coerceIn(0, origText.length)
            }
            val origLen = origText.length
            val transToOrigList = mutableListOf<Int>()
            var origIdx = 0
            while (origIdx < origLen) {
                val lineEndIdx = origText.indexOf('\n', startIndex = origIdx).let { if (it >= 0) it else origLen }
                val lineLength = lineEndIdx - origIdx
                val lineContent = origText.substring(origIdx, lineEndIdx)
                val (_, symbolLen) = HeadingLogic.getHeadingLevelAndLength(lineContent)

                for (i in symbolLen until lineLength) {
                    transToOrigList.add(origIdx + i)
                }
                if (lineEndIdx < origLen) {
                    transToOrigList.add(lineEndIdx)
                    origIdx = lineEndIdx + 1
                } else {
                    origIdx = lineEndIdx
                }
            }
            transToOrigList.add(origLen)
            if (transToOrigList.isEmpty()) return 0
            val clamped = transOffset.coerceIn(0, transToOrigList.lastIndex)
            return transToOrigList[clamped].coerceIn(0, origLen)
        }
    }
}


