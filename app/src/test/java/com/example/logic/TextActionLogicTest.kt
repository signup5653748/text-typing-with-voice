package com.example.logic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.ActionButton
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextActionLogicTest {

    @Test
    fun testEnterAction() {
        val initial = TextFieldValue("Hello World", TextRange(5))
        val result = TextActionLogic.handleAction(ActionButton.ENTER, initial)
        assertEquals("Hello\n World", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    @Test
    fun testDeleteAction_noSelection() {
        val initial = TextFieldValue("Hello World", TextRange(5))
        val result = TextActionLogic.handleAction(ActionButton.DELETE, initial)
        assertEquals("Hell World", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test
    fun testDeleteAction_withSelection() {
        val initial = TextFieldValue("Hello World", TextRange(0, 5))
        val result = TextActionLogic.handleAction(ActionButton.DELETE, initial)
        assertEquals(" World", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun testPasteAction() {
        val initial = TextFieldValue("Hello World", TextRange(5))
        val result = TextActionLogic.handleAction(ActionButton.PASTE, initial, clipboardText = " My")
        assertEquals("Hello My World", result.text)
        assertEquals(TextRange(8), result.selection)
    }
}
