package com.example.logic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.ActionButton
import kotlin.math.max
import kotlin.math.min

object TextActionLogic {
    fun handleAction(
        action: ActionButton,
        currentValue: TextFieldValue,
        transientRange: TextRange? = null,
        isWordMode: Boolean = false,
        clipboardText: String? = null,
        onCopy: (String) -> Unit = {}
    ): TextFieldValue {
        val text = currentValue.text
        var selStart = min(currentValue.selection.start, currentValue.selection.end).coerceIn(0, text.length)
        var selEnd = max(currentValue.selection.start, currentValue.selection.end).coerceIn(0, text.length)
        var hasSelection = selStart != selEnd

        // If no explicit selection but user navigated to a word/unit (transient highlight active)
        if (!hasSelection && transientRange != null && transientRange.start != transientRange.end) {
            val tStart = min(transientRange.start, transientRange.end).coerceIn(0, text.length)
            val tEnd = max(transientRange.start, transientRange.end).coerceIn(0, text.length)
            if (tStart != tEnd) {
                selStart = tStart
                selEnd = tEnd
                hasSelection = true
            }
        }

        return when (action) {
            ActionButton.ENTER -> {
                val newString = text.substring(0, selStart) + "\n" + text.substring(selEnd)
                TextFieldValue(
                    text = newString,
                    selection = TextRange(selStart + 1, selStart + 1),
                    composition = null
                )
            }
            ActionButton.DELETE -> {
                if (hasSelection) {
                    val newString = text.substring(0, selStart) + text.substring(selEnd)
                    TextFieldValue(
                        text = newString,
                        selection = TextRange(selStart, selStart),
                        composition = null
                    )
                } else if (isWordMode && selStart > 0) {
                    val wordRange = CursorLogic.getWordRangeAt(text, (selStart - 1).coerceAtLeast(0))
                    val start = wordRange.start.coerceIn(0, text.length)
                    val end = wordRange.end.coerceIn(0, text.length)
                    if (start < end) {
                        val newString = text.substring(0, start) + text.substring(end)
                        TextFieldValue(
                            text = newString,
                            selection = TextRange(start, start),
                            composition = null
                        )
                    } else {
                        val newString = text.substring(0, selStart - 1) + text.substring(selStart)
                        TextFieldValue(
                            text = newString,
                            selection = TextRange(selStart - 1, selStart - 1),
                            composition = null
                        )
                    }
                } else if (selStart > 0) {
                    val newString = text.substring(0, selStart - 1) + text.substring(selStart)
                    TextFieldValue(
                        text = newString,
                        selection = TextRange(selStart - 1, selStart - 1),
                        composition = null
                    )
                } else if (text.isNotEmpty() && selStart == 0) {
                    val newString = text.substring(1)
                    TextFieldValue(
                        text = newString,
                        selection = TextRange(0, 0),
                        composition = null
                    )
                } else {
                    currentValue
                }
            }
            ActionButton.CUT -> {
                if (hasSelection) {
                    val cutText = text.substring(selStart, selEnd)
                    onCopy(cutText)
                    val newString = text.substring(0, selStart) + text.substring(selEnd)
                    TextFieldValue(
                        text = newString,
                        selection = TextRange(selStart, selStart),
                        composition = null
                    )
                } else {
                    currentValue
                }
            }
            ActionButton.COPY -> {
                if (hasSelection) {
                    val copyText = text.substring(selStart, selEnd)
                    onCopy(copyText)
                }
                currentValue
            }
            ActionButton.PASTE -> {
                if (!clipboardText.isNullOrEmpty()) {
                    val newString = text.substring(0, selStart) + clipboardText + text.substring(selEnd)
                    val newCaret = selStart + clipboardText.length
                    TextFieldValue(
                        text = newString,
                        selection = TextRange(newCaret, newCaret),
                        composition = null
                    )
                } else {
                    currentValue
                }
            }
            else -> currentValue
        }
    }
}
