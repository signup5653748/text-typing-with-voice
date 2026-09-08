package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ActionButton
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.ui.components.ActionButtonGrid
import com.example.ui.components.ArrowKeyCluster
import com.example.ui.components.SelectionHighlightTransformation
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacePopup(viewModel: EditorViewModel) {
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val partialResults by viewModel.speechWrapper.partialResults.collectAsState()
    val finalResult by viewModel.speechWrapper.finalResult.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var previewText by remember { mutableStateOf(TextFieldValue("")) }
    var transientHighlight by remember { mutableStateOf<TextRange?>(null) }

    // When final result arrives: set text and automatically read aloud
    LaunchedEffect(finalResult) {
        if (finalResult != null && finalResult!!.isNotBlank()) {
            val textToInsert = finalResult!!.trim()
            val start = previewText.selection.min
            val end = previewText.selection.max
            val newString = if (previewText.text.isEmpty()) {
                textToInsert
            } else {
                previewText.text.substring(0, start) + textToInsert + previewText.text.substring(end)
            }
            previewText = TextFieldValue(newString, TextRange(start + textToInsert.length))
            transientHighlight = null
            // Automatically speak out the captured transcription for audible verification
            viewModel.ttsWrapper.speakFeedback(textToInsert)
            viewModel.speechWrapper.clearResults()
        }
    }

    var kActive by remember { mutableStateOf(false) }
    var pActive by remember { mutableStateOf(false) }
    var selActive by remember { mutableStateOf(false) }
    var selAnchor by remember { mutableStateOf<Int?>(null) }
    var idealX by remember { mutableStateOf<Float?>(null) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showPopupMoreSheet by remember { mutableStateOf(false) }

    // Caret blink animation
    val cursorAlpha = remember { Animatable(1f) }
    LaunchedEffect(previewText.selection, previewText.text) {
        cursorAlpha.snapTo(1f)
        while (true) {
            delay(530)
            cursorAlpha.animateTo(0f, animationSpec = tween(120))
            delay(200)
            cursorAlpha.animateTo(1f, animationSpec = tween(120))
        }
    }

    // Pulse animation for recording badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isListening) viewModel.speechWrapper.stopListening()
            else viewModel.speechWrapper.startListening(settings.voiceLanguage)
        }
    }

    val highlightColor = Color(settings.highlightColorHex)
    val buttonOrderList = remember(settings.buttonOrder) {
        settings.buttonOrder.split(",").filter { it.isNotBlank() }
    }

    fun handleArrow(direction: ArrowDirection) {
        val prevCaret = previewText.selection.end
        val res = CursorLogic.handleArrow(
            value = previewText,
            direction = direction,
            isCharacterMode = kActive,
            isParagraphMode = pActive,
            isSelActive = selActive,
            layoutResult = layoutResult,
            currentIdealX = idealX,
            currentSelAnchor = selAnchor
        )
        previewText = res.value
        idealX = res.idealX
        selAnchor = res.selAnchor
        transientHighlight = res.transientHighlightRange

        val newCaret = res.value.selection.end
        val text = previewText.text
        if (text.isNotEmpty() && newCaret != prevCaret) {
            when (direction) {
                ArrowDirection.LEFT, ArrowDirection.RIGHT -> {
                    if (kActive) {
                        val charIdx = if (direction == ArrowDirection.LEFT) newCaret else (newCaret - 1).coerceAtLeast(0)
                        if (charIdx in text.indices) {
                            val ch = text[charIdx]
                            viewModel.ttsWrapper.speakFeedback(if (ch == ' ') "Space" else ch.toString())
                        }
                    } else {
                        val wordIdx = if (direction == ArrowDirection.LEFT) newCaret else (newCaret - 1).coerceAtLeast(0)
                        val range = CursorLogic.getWordRangeAt(text, wordIdx)
                        if (range.start < range.end && range.end <= text.length) {
                            val w = text.substring(range.start, range.end).trim()
                            if (w.isNotBlank()) viewModel.ttsWrapper.speakFeedback(w)
                        }
                    }
                }
                ArrowDirection.UP, ArrowDirection.DOWN -> {
                    val line = layoutResult?.getLineForOffset(newCaret.coerceIn(0, text.length)) ?: 0
                    val range = if (layoutResult != null) CursorLogic.getLineRange(text, layoutResult!!, line) else CursorLogic.getFallbackLineRange(text, newCaret)
                    if (range.start < range.end && range.end <= text.length) {
                        val l = text.substring(range.start, range.end).trim()
                        if (l.isNotBlank()) viewModel.ttsWrapper.speakFeedback(l)
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeReplacePopup() },
        containerColor = Color(0xFF141926),
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Voice Replace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFECEFF8)
                    )

                    // Recording state badge
                    if (isListening) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF4B6E).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4B6E).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((8 * pulseScale).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF4B6E))
                                )
                                Text(
                                    "Listening...",
                                    color = Color(0xFFFF6B8A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = { viewModel.closeReplacePopup() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8FA7D8))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sandbox preview text area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, if (isListening) Color(0xFF56D0DE) else Color(0xFF242E44), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                if (previewText.text.isEmpty()) {
                    if (isListening && partialResults.isNotBlank()) {
                        Text(
                            text = partialResults,
                            color = Color(0xFF56D0DE),
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        Text(
                            if (isListening) "Listening... speak replacement text" else "Speak or edit replacement text...",
                            color = if (isListening) Color(0xFF56D0DE) else Color(0xFF6B7280),
                            fontSize = 17.sp
                        )
                    }
                }

                val brightSelection = TextSelectionColors(
                    handleColor = highlightColor,
                    backgroundColor = highlightColor.copy(alpha = 0.55f)
                )
                CompositionLocalProvider(LocalTextSelectionColors provides brightSelection) {
                    BasicTextField(
                        value = previewText,
                        onValueChange = { 
                            previewText = it
                            if (selActive) selActive = false
                            selAnchor = null
                            idealX = null
                            transientHighlight = null
                        },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            color = Color(0xFFECEEF2),
                            fontSize = 17.sp,
                            lineHeight = 25.sp
                        ),
                        visualTransformation = remember(previewText.selection, transientHighlight, highlightColor) {
                            SelectionHighlightTransformation(
                                selection = previewText.selection,
                                transientHighlight = transientHighlight,
                                highlightColor = highlightColor.copy(alpha = 0.65f),
                                highlightedTextColor = Color.White
                            )
                        },
                        cursorBrush = SolidColor(Color.Transparent),
                        onTextLayout = { layoutResult = it }
                    )
                }

                Canvas(modifier = Modifier.matchParentSize()) {
                    val layout = layoutResult
                    if (layout != null) {
                        val caret = previewText.selection.end.coerceIn(0, previewText.text.length)
                        val rect = layout.getCursorRect(caret)
                        drawRoundRect(
                            color = highlightColor.copy(alpha = cursorAlpha.value),
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(2.5.dp.toPx(), rect.height),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Control keypad in popup sandbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButtonGrid(
                    onActionClick = { action ->
                        when (action) {
                            ActionButton.K -> {
                                kActive = !kActive
                                viewModel.ttsWrapper.speakFeedback(if (kActive) "Character mode" else "Word mode")
                            }
                            ActionButton.P -> {
                                pActive = !pActive
                                viewModel.ttsWrapper.speakFeedback(if (pActive) "Paragraph mode" else "Line mode")
                            }
                            else -> {
                                val clipboardText = if (action == ActionButton.PASTE) viewModel.pasteFromClipboard() else null
                                previewText = com.example.logic.TextActionLogic.handleAction(
                                    action = action,
                                    currentValue = previewText,
                                    clipboardText = clipboardText,
                                    onCopy = { viewModel.copyToClipboard(it) }
                                )
                                when (action) {
                                    ActionButton.CUT -> viewModel.ttsWrapper.speakFeedback("Cut")
                                    ActionButton.COPY -> viewModel.ttsWrapper.speakFeedback("Copied")
                                    ActionButton.DELETE -> viewModel.ttsWrapper.speakFeedback("Deleted")
                                    ActionButton.PASTE -> viewModel.ttsWrapper.speakFeedback("Pasted")
                                    ActionButton.ENTER -> viewModel.ttsWrapper.speakFeedback("Enter")
                                    else -> {}
                                }
                                selActive = false
                                selAnchor = null
                                idealX = null
                                transientHighlight = null
                            }
                        }
                    },
                    onMoreClick = { showPopupMoreSheet = true },
                    buttonOrder = buttonOrderList,
                    sizeMultiplier = settings.buttonSizeMultiplier,
                    modifier = Modifier.weight(1f)
                )

                FloatingMicButton(
                    isListening = isListening,
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            if (isListening) viewModel.speechWrapper.stopListening()
                            else viewModel.speechWrapper.startListening(settings.voiceLanguage)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )

                ArrowKeyCluster(
                    selActive = selActive,
                    scale = settings.arrowSize * 0.85f,
                    onMoveUp = { handleArrow(ArrowDirection.UP) },
                    onMoveDown = { handleArrow(ArrowDirection.DOWN) },
                    onMoveLeft = { handleArrow(ArrowDirection.LEFT) },
                    onMoveRight = { handleArrow(ArrowDirection.RIGHT) },
                    onToggleSel = { 
                        selActive = !selActive
                        if (selActive) {
                            selAnchor = previewText.selection.end
                            transientHighlight = null
                            previewText = previewText.copy(selection = TextRange(selAnchor!!, selAnchor!!))
                            viewModel.ttsWrapper.speakFeedback("Selection mode on")
                        } else {
                            selAnchor = null
                            val c = previewText.selection.end
                            previewText = previewText.copy(selection = TextRange(c, c))
                            transientHighlight = null
                            viewModel.ttsWrapper.speakFeedback("Selection mode off")
                        }
                    },
                    activeHighlightColor = highlightColor,
                    modifier = Modifier.wrapContentWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons: Cancel and Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.closeReplacePopup() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8FA7D8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26324A))
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { viewModel.applyReplace(previewText.text) },
                    enabled = previewText.text.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply")
                }
            }
        }
    }
}
