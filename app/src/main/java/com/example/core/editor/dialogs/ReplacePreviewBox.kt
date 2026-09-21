package com.example.core.editor.dialogs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SelectionHighlightTransformation

val PopupEmptyTextToolbar = object : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun hide() {}
    override fun showMenu(
        rect: androidx.compose.ui.geometry.Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {}
}

@Composable
fun ReplacePreviewBox(
    previewText: TextFieldValue,
    onPreviewTextChange: (TextFieldValue) -> Unit,
    isListening: Boolean,
    partialResults: String,
    highlightColor: Color,
    cursorAlpha: Animatable<Float, AnimationVector1D>,
    transientHighlight: androidx.compose.ui.text.TextRange?,
    layoutResult: TextLayoutResult?,
    onTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C0D10))
            .border(1.5.dp, if (isListening) Color(0xFF56D0DE) else Color(0xFF242E44), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        if (previewText.text.isEmpty()) {
            if (isListening && partialResults.isNotBlank()) {
                Text(
                    text = partialResults,
                    color = Color(0xFF56D0DE),
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Text(
                    if (isListening) "Listening... speak replacement text" else "Speak or edit replacement text...",
                    color = if (isListening) Color(0xFF56D0DE) else Color(0xFF6B7280),
                    fontSize = 17.sp
                )
            }
        }

        val invisibleSelectionColors = remember {
            TextSelectionColors(
                handleColor = Color.Transparent,
                backgroundColor = Color.Transparent
            )
        }
        CompositionLocalProvider(
            LocalTextSelectionColors provides invisibleSelectionColors,
            LocalTextToolbar provides PopupEmptyTextToolbar
        ) {
            BasicTextField(
                value = previewText,
                onValueChange = onPreviewTextChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(
                    color = Color(0xFFECEEF2),
                    fontSize = 17.sp,
                    lineHeight = 25.sp
                ),
                visualTransformation = remember(previewText.selection, transientHighlight, highlightColor) {
                    val isLightHighlight = (highlightColor.red * 0.299f + highlightColor.green * 0.587f + highlightColor.blue * 0.114f) > 0.45f
                    SelectionHighlightTransformation(
                        selection = previewText.selection,
                        transientHighlight = transientHighlight,
                        highlightColor = highlightColor.copy(alpha = 0.7f),
                        highlightedTextColor = if (isLightHighlight) Color(0xFF0D111A) else Color.White
                    )
                },
                cursorBrush = SolidColor(Color.Transparent),
                onTextLayout = onTextLayout
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val layout = layoutResult
            if (layout != null && previewText.selection.collapsed) {
                try {
                    val maxLayoutOffset = layout.layoutInput.text.length
                    val caret = previewText.selection.end.coerceIn(0, maxLayoutOffset)
                    val rect = layout.getCursorRect(caret)
                    drawRoundRect(
                        color = highlightColor.copy(alpha = cursorAlpha.value),
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(2.5.dp.toPx(), rect.height),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                } catch (e: Exception) {
                    // Ignore race condition between text edit and layout calculation
                }
            }
        }
    }
}
