package com.example.logic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.math.max
import kotlin.math.min

enum class ArrowDirection { LEFT, RIGHT, UP, DOWN }

data class CursorResult(
    val value: TextFieldValue,
    val idealX: Float?,
    val selAnchor: Int?,
    val transientHighlightRange: TextRange?
)

object CursorLogic {

    fun handleArrow(
        value: TextFieldValue,
        direction: ArrowDirection,
        isCharacterMode: Boolean, // K active -> true (char by char), K off -> false (word by word)
        isParagraphMode: Boolean, // P active -> true (paragraph), P off -> false (line by line)
        isSelActive: Boolean,
        layoutResult: TextLayoutResult?,
        currentIdealX: Float?,
        currentSelAnchor: Int?
    ): CursorResult {
        val text = value.text
        val currentCaret = value.selection.end.coerceIn(0, text.length)
        var newIdealX = currentIdealX
        var newSelAnchor = currentSelAnchor

        var transientRange: TextRange? = null

        // 1. Calculate the new caret position & transient highlight unit
        val newCaret = when (direction) {
            ArrowDirection.LEFT -> {
                newIdealX = null
                if (isCharacterMode) {
                    val target = max(0, currentCaret - 1)
                    if (!isSelActive && text.isNotEmpty()) {
                        transientRange = TextRange(target, min(text.length, target + 1))
                    }
                    target
                } else {
                    val target = findPreviousWordBoundary(text, currentCaret)
                    val finalTarget = if (target >= currentCaret && currentCaret > 0) 0 else target
                    if (!isSelActive && text.isNotEmpty()) {
                        transientRange = getWordRangeAt(text, finalTarget)
                    }
                    finalTarget
                }
            }
            ArrowDirection.RIGHT -> {
                newIdealX = null
                if (isCharacterMode) {
                    val target = min(text.length, currentCaret + 1)
                    if (!isSelActive && text.isNotEmpty()) {
                        val hlStart = max(0, target - 1)
                        transientRange = TextRange(hlStart, target)
                    }
                    target
                } else {
                    val target = findNextWordBoundary(text, currentCaret)
                    val finalTarget = if (target <= currentCaret && currentCaret < text.length) text.length else target
                    if (!isSelActive && text.isNotEmpty()) {
                        transientRange = getWordRangeAt(text, if (finalTarget > 0) finalTarget - 1 else 0)
                    }
                    finalTarget
                }
            }
            ArrowDirection.UP -> {
                if (isParagraphMode) {
                    newIdealX = null
                    val target = findPreviousParagraphStart(text, currentCaret)
                    val finalTarget = if (target >= currentCaret && currentCaret > 0) 0 else target
                    if (!isSelActive && text.isNotEmpty()) {
                        transientRange = getParagraphRangeAt(text, finalTarget)
                    }
                    finalTarget
                } else if (layoutResult != null && layoutResult.lineCount > 0) {
                    val currentLine = layoutResult.getLineForOffset(currentCaret.coerceIn(0, text.length))
                    val prevLine = (currentLine - 1).coerceAtLeast(0)
                    val x = currentIdealX ?: layoutResult.getHorizontalPosition(currentCaret.coerceIn(0, text.length), true)
                    newIdealX = x

                    val target = if (currentLine <= 0) {
                        0
                    } else {
                        val lineTop = layoutResult.getLineTop(prevLine)
                        val calculatedTarget = layoutResult.getOffsetForPosition(Offset(x, lineTop + 1f)).coerceIn(0, text.length)
                        if (calculatedTarget >= currentCaret) {
                            val prevLineEnd = layoutResult.getLineEnd(prevLine)
                            if (prevLineEnd < currentCaret) {
                                prevLineEnd
                            } else if (prevLine == 0) {
                                0
                            } else {
                                (currentCaret - 1).coerceAtLeast(0)
                            }
                        } else {
                            calculatedTarget
                        }
                    }
                    if (!isSelActive) {
                        transientRange = getLineRange(text, layoutResult, prevLine)
                    }
                    target
                } else {
                    newIdealX = null
                    val target = findPreviousLineOffset(text, currentCaret)
                    val finalTarget = if (target >= currentCaret && currentCaret > 0) 0 else target
                    if (!isSelActive) {
                        transientRange = getFallbackLineRange(text, finalTarget)
                    }
                    finalTarget
                }
            }
            ArrowDirection.DOWN -> {
                if (isParagraphMode) {
                    newIdealX = null
                    val target = findNextParagraphStart(text, currentCaret)
                    val finalTarget = if (target <= currentCaret && currentCaret < text.length) text.length else target
                    if (!isSelActive && text.isNotEmpty()) {
                        transientRange = getParagraphRangeAt(text, finalTarget)
                    }
                    finalTarget
                } else if (layoutResult != null && layoutResult.lineCount > 0) {
                    val currentLine = layoutResult.getLineForOffset(currentCaret.coerceIn(0, text.length))
                    val nextLine = (currentLine + 1).coerceAtMost(layoutResult.lineCount - 1)
                    val x = currentIdealX ?: layoutResult.getHorizontalPosition(currentCaret.coerceIn(0, text.length), true)
                    newIdealX = x

                    val target = if (currentLine >= layoutResult.lineCount - 1) {
                        text.length
                    } else {
                        val lineTop = layoutResult.getLineTop(nextLine)
                        val calculatedTarget = layoutResult.getOffsetForPosition(Offset(x, lineTop + 1f)).coerceIn(0, text.length)
                        if (calculatedTarget <= currentCaret) {
                            val nextLineStart = layoutResult.getLineStart(nextLine)
                            if (nextLineStart > currentCaret) {
                                nextLineStart
                            } else if (nextLine == layoutResult.lineCount - 1) {
                                text.length
                            } else {
                                (currentCaret + 1).coerceAtMost(text.length)
                            }
                        } else {
                            calculatedTarget
                        }
                    }
                    if (!isSelActive) {
                        transientRange = getLineRange(text, layoutResult, nextLine)
                    }
                    target
                } else {
                    newIdealX = null
                    val target = findNextLineOffset(text, currentCaret)
                    val finalTarget = if (target <= currentCaret && currentCaret < text.length) text.length else target
                    if (!isSelActive) {
                        transientRange = getFallbackLineRange(text, finalTarget)
                    }
                    finalTarget
                }
            }
        }.coerceIn(0, text.length)

        // 2. Selection calculation
        val finalSelection: TextRange
        if (isSelActive) {
            val anchor = currentSelAnchor ?: currentCaret
            newSelAnchor = anchor
            finalSelection = TextRange(anchor, newCaret)
            transientRange = null // SEL active -> no transient highlight, real selection only
        } else {
            newSelAnchor = null
            finalSelection = TextRange(newCaret, newCaret)
        }

        return CursorResult(
            value = value.copy(selection = finalSelection, composition = null),
            idealX = newIdealX,
            selAnchor = newSelAnchor,
            transientHighlightRange = transientRange
        )
    }

    fun findPreviousWordBoundary(text: String, currentOffset: Int): Int {
        if (currentOffset <= 0) return 0
        var i = currentOffset.coerceIn(0, text.length)

        // Skip whitespace immediately before cursor
        while (i > 0 && text[i - 1].isWhitespace()) {
            i--
        }

        // Move to start of previous word
        while (i > 0 && !text[i - 1].isWhitespace()) {
            i--
        }
        return i
    }

    fun findNextWordBoundary(text: String, currentOffset: Int): Int {
        val len = text.length
        if (currentOffset >= len) return len
        var i = currentOffset.coerceIn(0, len)

        // Skip current word
        while (i < len && !text[i].isWhitespace()) {
            i++
        }

        // Skip whitespace following the word
        while (i < len && text[i].isWhitespace()) {
            i++
        }
        return i
    }

    fun getWordRangeAt(text: String, offset: Int): TextRange {
        if (text.isEmpty()) return TextRange(0, 0)
        val pos = offset.coerceIn(0, text.length)
        var start = if (pos < text.length && !text[pos].isWhitespace()) pos else (pos - 1).coerceAtLeast(0)
        while (start > 0 && !text[start - 1].isWhitespace()) {
            start--
        }
        var end = start
        while (end < text.length && !text[end].isWhitespace()) {
            end++
        }
        return if (start < end) TextRange(start, end) else TextRange(pos, min(text.length, pos + 1))
    }

    fun findParagraphStart(text: String, offset: Int): Int {
        if (text.isEmpty()) return 0
        val pos = offset.coerceIn(0, text.length)
        if (pos == 0) return 0
        val prevBreak = text.lastIndexOf("\n\n", (pos - 1).coerceAtLeast(0))
        return if (prevBreak == -1) {
            0
        } else {
            var start = prevBreak + 2
            while (start < text.length && text[start] == '\n') {
                start++
            }
            start
        }
    }

    fun findPreviousParagraphStart(text: String, offset: Int): Int {
        if (text.isEmpty() || offset <= 0) return 0
        val currentStart = findParagraphStart(text, offset)
        if (offset > currentStart) {
            return currentStart
        }
        val searchPos = (currentStart - 3).coerceAtLeast(0)
        val prevBreak = text.lastIndexOf("\n\n", searchPos)
        return if (prevBreak == -1) {
            0
        } else {
            var start = prevBreak + 2
            while (start < text.length && text[start] == '\n') {
                start++
            }
            start
        }
    }

    fun findNextParagraphStart(text: String, offset: Int): Int {
        val len = text.length
        if (offset >= len) return len
        val pos = offset.coerceIn(0, len)
        val nextBreak = text.indexOf("\n\n", pos)
        if (nextBreak == -1) {
            return len
        }
        var start = nextBreak + 2
        while (start < len && text[start] == '\n') {
            start++
        }
        return start.coerceAtMost(len)
    }

    fun getParagraphRangeAt(text: String, offset: Int): TextRange {
        if (text.isEmpty()) return TextRange(0, 0)
        val start = findParagraphStart(text, offset)
        val nextBreak = text.indexOf("\n\n", start)
        val end = if (nextBreak != -1) nextBreak else text.length
        return TextRange(start, end)
    }

    fun getLineRange(text: String, layoutResult: TextLayoutResult, lineIndex: Int): TextRange {
        val boundedLine = lineIndex.coerceIn(0, (layoutResult.lineCount - 1).coerceAtLeast(0))
        val start = layoutResult.getLineStart(boundedLine)
        var end = layoutResult.getLineEnd(boundedLine)
        // Trim trailing newline if we want text span or keep full line
        if (end > start && end <= text.length && text[end - 1] == '\n') {
            end -= 1
        }
        return TextRange(start, end)
    }

    fun getFallbackLineRange(text: String, offset: Int): TextRange {
        if (text.isEmpty()) return TextRange(0, 0)
        val pos = offset.coerceIn(0, text.length)
        val lastNewline = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0))
        val start = if (lastNewline == -1) 0 else lastNewline + 1
        val nextNewline = text.indexOf('\n', pos)
        val end = if (nextNewline == -1) text.length else nextNewline
        return TextRange(start, end)
    }

    private fun findPreviousLineOffset(text: String, currentOffset: Int): Int {
        if (currentOffset <= 0) return 0
        val lastNewline = text.lastIndexOf('\n', currentOffset - 1)
        if (lastNewline == -1) return 0
        val lineCol = currentOffset - (lastNewline + 1)
        val prevLineNewline = text.lastIndexOf('\n', lastNewline - 1)
        val prevLineStart = if (prevLineNewline == -1) 0 else prevLineNewline + 1
        val prevLineLen = lastNewline - prevLineStart
        return prevLineStart + min(lineCol, prevLineLen)
    }

    private fun findNextLineOffset(text: String, currentOffset: Int): Int {
        val len = text.length
        if (currentOffset >= len) return len
        val currentLineStart = text.lastIndexOf('\n', currentOffset - 1).let { if (it == -1) 0 else it + 1 }
        val lineCol = currentOffset - currentLineStart
        val nextNewline = text.indexOf('\n', currentOffset)
        if (nextNewline == -1) return len
        val nextLineStart = nextNewline + 1
        val lineAfterNext = text.indexOf('\n', nextLineStart).let { if (it == -1) len else it }
        val nextLineLen = lineAfterNext - nextLineStart
        return min(nextLineStart + lineCol, lineAfterNext)
    }
}
