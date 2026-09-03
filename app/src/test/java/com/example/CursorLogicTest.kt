package com.example

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.ActionButton
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.logic.TextActionLogic
import org.junit.Assert.*
import org.junit.Test

class CursorLogicTest {

    @Test
    fun testMovementUnitsLeftRight() {
        val text = "Hello brave new world"
        val initial = TextFieldValue(text, TextRange(0)) // start

        // 1. Default (K off): word by word
        val step1 = CursorLogic.handleArrow(
            value = initial,
            direction = ArrowDirection.RIGHT,
            isCharacterMode = false, // K off
            isParagraphMode = false,
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        // Caret moves word by word to index 6 ("Hello ")
        assertEquals(6, step1.value.selection.end)
        assertEquals(step1.value.selection.start, step1.value.selection.end) // No real selection
        assertNotNull(step1.transientHighlightRange) // Has transient highlight

        // 2. K active (K on): character by character
        val step2 = CursorLogic.handleArrow(
            value = step1.value,
            direction = ArrowDirection.RIGHT,
            isCharacterMode = true, // K on
            isParagraphMode = false,
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        assertEquals(7, step2.value.selection.end)
        assertEquals(step2.value.selection.start, step2.value.selection.end)

        // Left with K on (char by char)
        val step3 = CursorLogic.handleArrow(
            value = step2.value,
            direction = ArrowDirection.LEFT,
            isCharacterMode = true, // K on
            isParagraphMode = false,
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        assertEquals(6, step3.value.selection.end)

        // Left with K off (word by word)
        val step4 = CursorLogic.handleArrow(
            value = step3.value,
            direction = ArrowDirection.LEFT,
            isCharacterMode = false, // K off
            isParagraphMode = false,
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        assertEquals(0, step4.value.selection.end)
    }

    @Test
    fun testMovementUnitsUpDownParagraphMode() {
        val text = "Paragraph one with text.\nStill para one.\n\nParagraph two begins here.\nStill para two.\n\nParagraph three."
        val initial = TextFieldValue(text, TextRange(0))

        // Down arrow in paragraph mode (P on): must land at START of next paragraph
        val p1 = CursorLogic.handleArrow(
            value = initial,
            direction = ArrowDirection.DOWN,
            isCharacterMode = false,
            isParagraphMode = true, // P on
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        val paraTwoStart = text.indexOf("Paragraph two begins here.")
        assertEquals(paraTwoStart, p1.value.selection.end)
        assertEquals(p1.value.selection.start, p1.value.selection.end) // SEL not active

        // Down arrow again -> land at START of Paragraph three
        val p2 = CursorLogic.handleArrow(
            value = p1.value,
            direction = ArrowDirection.DOWN,
            isCharacterMode = false,
            isParagraphMode = true, // P on
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        val paraThreeStart = text.indexOf("Paragraph three.")
        assertEquals(paraThreeStart, p2.value.selection.end)

        // Up arrow in paragraph mode (P on): must land at START of previous paragraph symmetrically
        val pUp1 = CursorLogic.handleArrow(
            value = p2.value,
            direction = ArrowDirection.UP,
            isCharacterMode = false,
            isParagraphMode = true, // P on
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        assertEquals(paraTwoStart, pUp1.value.selection.end)

        val pUp2 = CursorLogic.handleArrow(
            value = pUp1.value,
            direction = ArrowDirection.UP,
            isCharacterMode = false,
            isParagraphMode = true, // P on
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        assertEquals(0, pUp2.value.selection.end)
    }

    @Test
    fun testRealSelectionWithSelActive() {
        val text = "First word Second word Third word"
        val initial = TextFieldValue(text, TextRange(0))

        // SEL active: Move Right (word by word) -> selection extends from anchor 0 to end of First word
        val s1 = CursorLogic.handleArrow(
            value = initial,
            direction = ArrowDirection.RIGHT,
            isCharacterMode = false,
            isParagraphMode = false,
            isSelActive = true,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = 0
        )
        assertEquals(0, s1.value.selection.start)
        assertEquals(6, s1.value.selection.end) // "First "
        assertNull(s1.transientHighlightRange) // No transient highlight when real selection is active

        // Switch to character mode mid-selection and move right 1 char
        val s2 = CursorLogic.handleArrow(
            value = s1.value,
            direction = ArrowDirection.RIGHT,
            isCharacterMode = true, // Switch to char mode
            isParagraphMode = false,
            isSelActive = true,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = s1.selAnchor // Anchor preserved!
        )
        assertEquals(0, s2.value.selection.start)
        assertEquals(7, s2.value.selection.end)
    }

    @Test
    fun testEnterWithActiveSelection() {
        val text = "Hello beautiful world"
        // Selection on "beautiful" (indices 6 to 15)
        val selected = TextFieldValue(text, TextRange(6, 15))

        val result = TextActionLogic.handleAction(
            action = ActionButton.ENTER,
            currentValue = selected
        )

        assertEquals("Hello \n world", result.text)
        assertEquals(7, result.selection.start)
        assertEquals(7, result.selection.end)
    }

    @Test
    fun testDeleteWithActiveSelection() {
        val text = "Hello beautiful world"
        val selected = TextFieldValue(text, TextRange(6, 15))

        val result = TextActionLogic.handleAction(
            action = ActionButton.DELETE,
            currentValue = selected
        )

        assertEquals("Hello  world", result.text)
        assertEquals(6, result.selection.start)
        assertEquals(6, result.selection.end)
    }

    @Test
    fun testCutAndPasteWithSelection() {
        val text = "Cut this sample"
        val selected = TextFieldValue(text, TextRange(4, 8)) // "this"
        var copied = ""

        val cutResult = TextActionLogic.handleAction(
            action = ActionButton.CUT,
            currentValue = selected,
            onCopy = { copied = it }
        )
        assertEquals("this", copied)
        assertEquals("Cut  sample", cutResult.text)
        assertEquals(4, cutResult.selection.start)
        assertEquals(4, cutResult.selection.end)

        // Paste at position 4
        val pasteResult = TextActionLogic.handleAction(
            action = ActionButton.PASTE,
            currentValue = cutResult,
            clipboardText = "my"
        )
        assertEquals("Cut my sample", pasteResult.text)
        assertEquals(6, pasteResult.selection.start)
        assertEquals(6, pasteResult.selection.end)
    }
}
