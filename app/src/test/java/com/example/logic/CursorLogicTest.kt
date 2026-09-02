package com.example.logic

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CursorLogicTest {

    @Test
    fun testMoveLeft_characterMode() {
        val initial = TextFieldValue("Hello World", TextRange(5))
        val (result, _, _) = CursorLogic.handleArrow(
            value = initial,
            direction = ArrowDirection.LEFT,
            isWordMode = false,
            isLineMode = false,
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        assertEquals(TextRange(4, 4), result.selection)
    }

    @Test
    fun testMoveLeft_wordMode() {
        val initial = TextFieldValue("Hello World", TextRange(6))
        val (result, _, _) = CursorLogic.handleArrow(
            value = initial,
            direction = ArrowDirection.LEFT,
            isWordMode = true,
            isLineMode = false,
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        // Should jump back to the start of "World" which is index 6. 
        // Wait, index 6 is 'W', moving left from 'W' boundary might go to 'Hello' boundary if we are at 6.
        // Let's test with position at 11 (end of World) -> should jump to 6.
        val endOfWorld = TextFieldValue("Hello World", TextRange(11))
        val (result2, _, _) = CursorLogic.handleArrow(
            value = endOfWorld,
            direction = ArrowDirection.LEFT,
            isWordMode = true,
            isLineMode = false,
            isSelActive = false,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = null
        )
        assertEquals(TextRange(6, 6), result2.selection)
    }

    @Test
    fun testSelectionMode() {
        val initial = TextFieldValue("Hello World", TextRange(5))
        val (result, _, newAnchor) = CursorLogic.handleArrow(
            value = initial,
            direction = ArrowDirection.LEFT,
            isWordMode = false,
            isLineMode = false,
            isSelActive = true,
            layoutResult = null,
            currentIdealX = null,
            currentSelAnchor = 5
        )
        // Anchor remains 5, caret moves to 4
        assertEquals(TextRange(5, 4), result.selection)
        assertEquals(5, newAnchor)
    }
}
