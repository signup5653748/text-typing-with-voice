package com.example.core.editor

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.ActionButton
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.logic.HeadingLogic
import com.example.logic.TextActionLogic

data class ArrowMoveResult(
    val newValue: TextFieldValue,
    val idealX: Float?,
    val selAnchor: Int?,
    val transientHighlightRange: TextRange?,
    val feedbackToSpeak: String?
)

/**
 * Handles cursor navigation, K (char/word) & P (line/paragraph) modes,
 * selection anchors, clipboard action handling, and spoken feedback text calculation.
 */
class EditorSelectionManager {
    var selAnchor: Int? = null
    var idealX: Float? = null

    fun resetCursorState(current: TextFieldValue): TextFieldValue {
        selAnchor = null
        idealX = null
        return if (current.selection.start != current.selection.end) {
            current.copy(
                selection = TextRange(current.selection.end, current.selection.end),
                composition = null
            )
        } else {
            current
        }
    }

    fun handleArrow(
        value: TextFieldValue,
        direction: ArrowDirection,
        isKActive: Boolean,
        isPActive: Boolean,
        isSelActive: Boolean,
        layoutResult: TextLayoutResult?
    ): ArrowMoveResult {
        val prevCaret = value.selection.end
        val res = CursorLogic.handleArrow(
            value = value,
            direction = direction,
            isCharacterMode = isKActive,
            isParagraphMode = isPActive,
            isSelActive = isSelActive,
            layoutResult = layoutResult,
            currentIdealX = idealX,
            currentSelAnchor = selAnchor
        )
        idealX = res.idealX
        selAnchor = res.selAnchor

        val newCaret = res.value.selection.end
        val feedback = calculateNavigationFeedback(
            text = res.value.text,
            direction = direction,
            prevCaret = prevCaret,
            newCaret = newCaret,
            isKActive = isKActive,
            isPActive = isPActive,
            layoutResult = layoutResult
        )

        return ArrowMoveResult(
            newValue = res.value,
            idealX = res.idealX,
            selAnchor = res.selAnchor,
            transientHighlightRange = res.transientHighlightRange,
            feedbackToSpeak = feedback
        )
    }

    private fun calculateNavigationFeedback(
        text: String,
        direction: ArrowDirection,
        prevCaret: Int,
        newCaret: Int,
        isKActive: Boolean,
        isPActive: Boolean,
        layoutResult: TextLayoutResult?
    ): String? {
        if (text.isEmpty()) return "Empty document"
        if (newCaret == prevCaret) {
            return if (newCaret <= 0) "Beginning of document" else "End of document"
        }

        return when (direction) {
            ArrowDirection.LEFT, ArrowDirection.RIGHT -> {
                if (isKActive) {
                    val charIdx = if (direction == ArrowDirection.LEFT) newCaret else (newCaret - 1).coerceAtLeast(0)
                    if (charIdx in text.indices) {
                        when (val ch = text[charIdx]) {
                            ' ' -> "Space"
                            '\n' -> "New line"
                            '\t' -> "Tab"
                            '.' -> "Dot"
                            ',' -> "Comma"
                            '!' -> "Exclamation mark"
                            '?' -> "Question mark"
                            ':' -> "Colon"
                            ';' -> "Semicolon"
                            '-' -> "Hyphen"
                            '(' -> "Open parenthesis"
                            ')' -> "Close parenthesis"
                            '\"' -> "Quote"
                            '\'' -> "Apostrophe"
                            '/' -> "Slash"
                            '\\' -> "Backslash"
                            '@' -> "At sign"
                            '#' -> "Hash"
                            '$' -> "Dollar"
                            '%' -> "Percent"
                            '&' -> "Ampersand"
                            '*' -> "Asterisk"
                            '+' -> "Plus"
                            '=' -> "Equals"
                            '<' -> "Less than"
                            '>' -> "Greater than"
                            else -> ch.toString()
                        }
                    } else null
                } else {
                    val wordRange = CursorLogic.getWordRangeAt(text, if (direction == ArrowDirection.LEFT) newCaret else (newCaret - 1).coerceAtLeast(0))
                    val word = if (wordRange.start < wordRange.end && wordRange.end <= text.length) {
                        text.substring(wordRange.start, wordRange.end).trim()
                    } else ""
                    if (word.isNotBlank()) word else "Space"
                }
            }
            ArrowDirection.UP, ArrowDirection.DOWN -> {
                if (isPActive) {
                    val pRange = CursorLogic.getParagraphRangeAt(text, newCaret)
                    val pText = if (pRange.start < pRange.end && pRange.end <= text.length) {
                        text.substring(pRange.start, pRange.end).trim()
                    } else ""
                    val cleanPText = HeadingLogic.stripLeadingSymbols(pText)
                    if (cleanPText.isNotBlank()) {
                        if (cleanPText.length > 60) cleanPText.substring(0, 60) + "..." else cleanPText
                    } else {
                        "Blank paragraph"
                    }
                } else {
                    val lineRange = if (layoutResult != null && layoutResult.lineCount > 0) {
                        val line = layoutResult.getLineForOffset(newCaret.coerceIn(0, text.length))
                        CursorLogic.getLineRange(text, layoutResult, line)
                    } else {
                        CursorLogic.getFallbackLineRange(text, newCaret)
                    }
                    val lineText = if (lineRange.start < lineRange.end && lineRange.end <= text.length) {
                        text.substring(lineRange.start, lineRange.end).trim()
                    } else ""
                    val cleanLineText = HeadingLogic.stripLeadingSymbols(lineText)
                    if (cleanLineText.isNotBlank()) cleanLineText else "Blank line"
                }
            }
        }
    }
}
