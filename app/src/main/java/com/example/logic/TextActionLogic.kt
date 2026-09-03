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
        clipboardText: String? = null,
        onCopy: (String) -> Unit = {}
    ): TextFieldValue {
        val text = currentValue.text
        val selStart = min(currentValue.selection.start, currentValue.selection.end).coerceIn(0, text.length)
        val selEnd = max(currentValue.selection.start, currentValue.selection.end).coerceIn(0, text.length)
        val hasSelection = selStart != selEnd

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
                } else if (selStart > 0) {
                    val newString = text.substring(0, selStart - 1) + text.substring(selStart)
                    TextFieldValue(
                        text = newString,
                        selection = TextRange(selStart - 1, selStart - 1),
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
