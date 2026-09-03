package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.junit.Test
import org.junit.Assert.assertEquals

class TestVisualTransformation {
    @Test
    fun testHighlightTransformation() {
        val original = "Hello world"
        val selStart = 0
        val selEnd = 5
        val transformed = buildAnnotatedString {
            append(original)
            if (selStart < selEnd && selEnd <= original.length) {
                addStyle(
                    SpanStyle(background = Color(0xFF6C8CFF).copy(alpha = 0.5f)),
                    selStart,
                    selEnd
                )
            }
        }
        assertEquals(original, transformed.text)
        assertEquals(1, transformed.spanStyles.size)
    }
}
