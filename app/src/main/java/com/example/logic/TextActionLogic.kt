package com.example.logic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.ActionButton

object TextActionLogic {
    fun handleAction(
        action: ActionButton,
        currentValue: TextFieldValue,
        clipboardText: String? = null,
        onCopy: (String) -> Unit = {}
    ): TextFieldValue {
        var newValue = currentValue
        when (action) {
            ActionButton.ENTER -> {
                val start = newValue.selection.min
                val end = newValue.selection.max
                val newString = newValue.text.substring(0, start) + "\n" + newValue.text.substring(end)
                newValue = newValue.copy(text = newString, selection = TextRange(start + 1), composition = null)
            }
            ActionButton.DELETE -> {
                if (newValue.selection.length > 0) {
                    val start = newValue.selection.min
                    val end = newValue.selection.max
                    val newString = newValue.text.substring(0, start) + newValue.text.substring(end)
                    newValue = newValue.copy(text = newString, selection = TextRange(start), composition = null)
                } else if (newValue.selection.min > 0) {
                    val start = newValue.selection.min - 1
                    val newString = newValue.text.substring(0, start) + newValue.text.substring(newValue.selection.min)
                    newValue = newValue.copy(text = newString, selection = TextRange(start), composition = null)
                }
            }
            ActionButton.CUT -> {
                if (newValue.selection.length > 0) {
                    val start = newValue.selection.min
                    val end = newValue.selection.max
                    onCopy(newValue.text.substring(start, end))
                    val newString = newValue.text.substring(0, start) + newValue.text.substring(end)
                    newValue = newValue.copy(text = newString, selection = TextRange(start), composition = null)
                }
            }
            ActionButton.COPY -> {
                if (newValue.selection.length > 0) {
                    val start = newValue.selection.min
                    val end = newValue.selection.max
                    onCopy(newValue.text.substring(start, end))
                }
            }
            ActionButton.PASTE -> {
                if (!clipboardText.isNullOrEmpty()) {
                    val start = newValue.selection.min
                    val end = newValue.selection.max
                    val newString = newValue.text.substring(0, start) + clipboardText + newValue.text.substring(end)
                    newValue = newValue.copy(text = newString, selection = TextRange(start + clipboardText.length), composition = null)
                }
            }
            else -> {}
        }
        return newValue
    }
}
