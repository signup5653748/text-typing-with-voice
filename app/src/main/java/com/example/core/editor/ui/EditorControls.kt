package com.example.core.editor.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsEntity
import com.example.ui.components.SelectionHighlightTransformation
import com.example.ui.components.instantClickable

private val EmptyTextToolbar = object : TextToolbar {
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
fun EditorTextArea(
    textValue: TextFieldValue,
    settings: SettingsEntity,
    kbLockActive: Boolean,
    transientHighlightRange: TextRange?,
    speechHighlightRange: TextRange?,
    onTextChanged: (TextFieldValue) -> Unit,
    onCaretTap: (Int) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightColor = remember(settings.highlightColorHex) { Color(settings.highlightColorHex) }
    val textColor = remember(settings.textColorHex) { Color(settings.textColorHex) }
    val textSize = settings.textSizeSp.sp
    val lineHeight = (settings.textSizeSp * 1.4f).sp

    val focusRequester = remember { FocusRequester() }
    var localLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isCursorVisible = !kbLockActive && textValue.selection.collapsed &&
            transientHighlightRange == null && speechHighlightRange == null

    val cachedOffsetMap = remember(textValue.text, settings.hideHeadingSymbols) {
        if (settings.hideHeadingSymbols && textValue.text.isNotEmpty()) {
            SelectionHighlightTransformation.computeOffsetMap(textValue.text)
        } else null
    }

    Box(
        modifier = modifier.then(
            if (kbLockActive) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        keyboardController?.hide()
                        localLayoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(tapOffset)
                            val origOffset = SelectionHighlightTransformation.transformedToOriginal(offset, cachedOffsetMap)
                            onCaretTap(origOffset)
                        }
                    }
                }
            } else {
                Modifier
            }
        )
    ) {
        val invisibleSelectionColors = remember {
            TextSelectionColors(
                handleColor = Color.Transparent,
                backgroundColor = Color.Transparent
            )
        }
        CompositionLocalProvider(
            LocalTextSelectionColors provides invisibleSelectionColors,
            LocalTextToolbar provides EmptyTextToolbar
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                readOnly = kbLockActive,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = textSize,
                    lineHeight = lineHeight
                ),
                visualTransformation = remember(
                    textValue.selection,
                    transientHighlightRange,
                    speechHighlightRange,
                    highlightColor,
                    settings.hideHeadingSymbols,
                    cachedOffsetMap
                ) {
                    val isLightHighlight = (highlightColor.red * 0.299f + highlightColor.green * 0.587f + highlightColor.blue * 0.114f) > 0.45f
                    SelectionHighlightTransformation(
                        selection = textValue.selection,
                        transientHighlight = transientHighlightRange,
                        speechHighlight = speechHighlightRange,
                        highlightColor = highlightColor.copy(alpha = 0.7f),
                        speechHighlightColor = Color(0xFF00E5FF),
                        highlightedTextColor = if (isLightHighlight) Color(0xFF0D111A) else Color.White,
                        hideHeadingSymbols = settings.hideHeadingSymbols,
                        cachedMapping = cachedOffsetMap
                    )
                },
                cursorBrush = SolidColor(Color.Transparent),
                onTextLayout = {
                    localLayoutResult = it
                    onTextLayout(it)
                }
            )
        }

        if (textValue.text.isEmpty()) {
            Text(
                "Type Something Here",
                color = textColor.copy(alpha = 0.4f),
                fontSize = textSize,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        if (isCursorVisible) {
            val transCaret = remember(textValue.selection.end, cachedOffsetMap, settings.hideHeadingSymbols) {
                if (settings.hideHeadingSymbols && cachedOffsetMap != null) {
                    SelectionHighlightTransformation.originalToTransformed(textValue.selection.end, cachedOffsetMap)
                } else {
                    textValue.selection.end
                }
            }
            BlinkingCursorOverlay(
                layout = localLayoutResult,
                caretOffset = transCaret,
                highlightColor = highlightColor,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
fun BlinkingCursorOverlay(
    layout: TextLayoutResult?,
    caretOffset: Int,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val cursorRect = remember(layout, caretOffset) {
        if (layout != null) {
            try {
                val maxLayoutOffset = layout.layoutInput.text.length
                val caret = caretOffset.coerceIn(0, maxLayoutOffset)
                layout.getCursorRect(caret)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    Canvas(modifier = modifier) {
        cursorRect?.let { rect ->
            drawRoundRect(
                color = highlightColor.copy(alpha = cursorAlpha),
                topLeft = Offset(rect.left, rect.top),
                size = Size(2.5.dp.toPx(), rect.height),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
fun QuickToggleButton(
    label: String,
    subLabel: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.2f) else Color(0xFF141C2B)
    val borderColor = if (isActive) activeColor else Color(0xFF222E44)
    val textColor = if (isActive) activeColor else Color(0xFFECEFF8)
    val subTextColor = if (isActive) activeColor else Color(0xFF8FA7D8)

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 11.sp)
            Text(text = subLabel, color = subTextColor, fontWeight = FontWeight.Medium, fontSize = 8.sp, lineHeight = 8.sp)
        }
    }
}

@Composable
fun QuickToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141C2B))
            .border(1.dp, Color(0xFF222E44), RoundedCornerShape(8.dp))
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF8FA7D8), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun FloatingReadButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPlaying) Color(0xFF2563EB) else Color(0xFF161E30)
    val contentColor = Color(0xFF8FA7D8)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, Color(0xFF26324A), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PlayArrow,
                contentDescription = "Read",
                tint = if (isPlaying) Color.White else contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "READ",
                color = if (isPlaying) Color.White else contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isListening) Color(0xFFFF4B6E) else Color(0xFF56D0DE)

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = Icons.Default.Mic, contentDescription = "Microphone", tint = Color.Black, modifier = Modifier.size(24.dp))
    }
}
