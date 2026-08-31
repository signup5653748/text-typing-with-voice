package com.example.logic

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.math.max
import kotlin.math.min

enum class ArrowDirection { LEFT, RIGHT, UP, DOWN }

object CursorLogic {

    fun handleArrow(
        value: TextFieldValue,
        direction: ArrowDirection,
        isWordMode: Boolean,
        isLineMode: Boolean,
        isSelActive: Boolean,
        layoutResult: TextLayoutResult?,
        currentIdealX: Float?,
        currentSelAnchor: Int?
    ): Triple<TextFieldValue, Float?, Int?> {
        val text = value.text
        val caret = value.selection.end
        var newIdealX = currentIdealX
        var newSelAnchor = currentSelAnchor
        
        // 1. Determine new logical caret position
        val newCaret = when (direction) {
            ArrowDirection.LEFT -> {
                newIdealX = null
                if (isWordMode) findPreviousWordBoundary(text, caret) else max(0, caret - 1)
            }
            ArrowDirection.RIGHT -> {
                newIdealX = null
                if (isWordMode) findNextWordBoundary(text, caret) else min(text.length, caret + 1)
            }
            ArrowDirection.UP -> {
                if (!isLineMode) {
                    findPreviousParagraphBoundary(text, caret)
                } else if (layoutResult != null) {
                    val currentLine = layoutResult.getLineForOffset(caret)
                    if (currentLine > 0) {
                        val prevLine = currentLine - 1
                        val x = currentIdealX ?: layoutResult.getHorizontalPosition(caret, true)
                        newIdealX = x
                        layoutResult.getOffsetForPosition(androidx.compose.ui.geometry.Offset(x, layoutResult.getLineTop(prevLine) + 1f))
                    } else 0
                } else caret
            }
            ArrowDirection.DOWN -> {
                if (!isLineMode) {
                    findNextParagraphBoundary(text, caret)
                } else if (layoutResult != null) {
                    val currentLine = layoutResult.getLineForOffset(caret)
                    if (currentLine < layoutResult.lineCount - 1) {
                        val nextLine = currentLine + 1
                        val x = currentIdealX ?: layoutResult.getHorizontalPosition(caret, true)
                        newIdealX = x
                        layoutResult.getOffsetForPosition(androidx.compose.ui.geometry.Offset(x, layoutResult.getLineTop(nextLine) + 1f))
                    } else text.length
                } else caret
            }
        }

        // 2. Determine anchor (start of visual block)
        val anchor = if (isSelActive) {
            currentSelAnchor ?: caret
        } else {
            caret // If SEL is off, transient highlight spans exactly the movement path
        }
        
        if (isSelActive && newSelAnchor == null) {
            newSelAnchor = anchor
        }

        // 3. Apply full-line snapping if moving Up/Down in P=Line mode
        val finalSelection = if (isLineMode && layoutResult != null && (direction == ArrowDirection.UP || direction == ArrowDirection.DOWN)) {
            val anchorLine = layoutResult.getLineForOffset(anchor)
            val caretLine = layoutResult.getLineForOffset(newCaret)
            
            val startLine = min(anchorLine, caretLine)
            val endLine = max(anchorLine, caretLine)
            
            val snappedStart = layoutResult.getLineStart(startLine)
            var snappedEnd = layoutResult.getLineEnd(endLine)
            
            // To ensure edge-to-edge highlight and correctly render blank lines,
            // we must include the trailing newline character in the selection if it exists.
            if (snappedEnd < text.length && text[snappedEnd] == '\n') {
                snappedEnd += 1
            }
            
            // If newCaret < anchor, or we are moving UP but on the same line, the selection is backwards.
            val isBackwards = newCaret < anchor || (newCaret == anchor && direction == ArrowDirection.UP)
            
            val snappedCaret = if (isBackwards) snappedStart else snappedEnd
            val snappedAnchor = if (isBackwards) snappedEnd else snappedStart
            
            if (isSelActive) {
                newSelAnchor = snappedAnchor // Permanently snap the anchor so it can be cleanly extended
            }
            
            TextRange(snappedAnchor, snappedCaret)
        } else {
            TextRange(anchor, newCaret)
        }

        return Triple(value.copy(selection = finalSelection), newIdealX, newSelAnchor)
    }

    private fun findPreviousWordBoundary(text: String, currentOffset: Int): Int {
        if (currentOffset <= 0) return 0
        var i = currentOffset - 1
        
        while (i > 0 && text[i].isWhitespace()) {
            i--
        }
        
        while (i > 0 && !text[i - 1].isWhitespace()) {
            i--
        }
        return i
    }

    private fun findNextWordBoundary(text: String, currentOffset: Int): Int {
        val len = text.length
        if (currentOffset >= len) return len
        var i = currentOffset
        
        while (i < len && !text[i].isWhitespace()) {
            i++
        }
        
        while (i < len && text[i].isWhitespace()) {
            i++
        }
        return i
    }

    private fun findPreviousParagraphBoundary(text: String, currentOffset: Int): Int {
        if (currentOffset <= 0) return 0
        var i = currentOffset - 2
        while (i >= 0) {
            val idx = text.lastIndexOf("\n\n", i)
            if (idx != -1) {
                val paraStart = idx + 2
                if (paraStart < currentOffset) {
                    return paraStart
                }
                i = idx - 1
            } else {
                return 0
            }
        }
        return 0
    }

    private fun findNextParagraphBoundary(text: String, currentOffset: Int): Int {
        val len = text.length
        if (currentOffset >= len) return len
        var i = currentOffset
        while (i < len) {
            val idx = text.indexOf("\n\n", i)
            if (idx != -1) {
                val paraStart = idx + 2
                if (paraStart > currentOffset) {
                    return paraStart
                }
                i = idx + 1
            } else {
                return len
            }
        }
        return len
    }
}
