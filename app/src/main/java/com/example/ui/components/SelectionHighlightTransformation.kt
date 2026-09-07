package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.max
import kotlin.math.min

class SelectionHighlightTransformation(
    private val selection: TextRange,
    private val transientHighlight: TextRange? = null,
    private val highlightColor: Color = Color(0xFFFFD600).copy(alpha = 0.55f)
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val effectiveRange = if (selection.length > 0) {
            selection
        } else if (transientHighlight != null && transientHighlight.length > 0) {
            transientHighlight
        } else {
            null
        }

        if (effectiveRange == null) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val start = min(effectiveRange.start, effectiveRange.end).coerceIn(0, text.length)
        val end = max(effectiveRange.start, effectiveRange.end).coerceIn(0, text.length)
        
        if (start >= end) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        
        val annotated = buildAnnotatedString {
            append(text.text)
            addStyle(
                SpanStyle(
                    background = highlightColor,
                    color = if (isLightColor(highlightColor)) Color.Black else Color.White
                ),
                start,
                end
            )
        }
        
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    private fun isLightColor(color: Color): Boolean {
        val r = color.red
        val g = color.green
        val b = color.blue
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance > 0.5
    }
}
