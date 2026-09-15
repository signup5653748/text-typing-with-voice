package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
import androidx.core.content.ContextCompat
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.ui.components.ArrowKeyCluster
import com.example.ui.components.SelectionHighlightTransformation
import kotlinx.coroutines.delay

private val PopupEmptyTextToolbar = object : TextToolbar {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacePopup(viewModel: EditorViewModel) {
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val partialResults by viewModel.speechWrapper.partialResults.collectAsState()
    val finalResult by viewModel.speechWrapper.finalResult.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var previewText by remember { mutableStateOf(TextFieldValue("")) }
    var transientHighlight by remember { mutableStateOf<TextRange?>(null) }

    // When final result arrives: append/insert text and automatically read aloud
    LaunchedEffect(finalResult) {
        if (finalResult != null && finalResult!!.isNotBlank()) {
            val textToInsert = finalResult!!.trim()
            val start = previewText.selection.min
            val end = previewText.selection.max
            val newString = if (previewText.text.isEmpty()) {
                textToInsert
            } else {
                val prefix = previewText.text.substring(0, start)
                val suffix = previewText.text.substring(end)
                // Add a space between words if needed
                val needsLeadingSpace = prefix.isNotEmpty() && !prefix.endsWith(" ") && !prefix.endsWith("\n")
                val textWithSpace = if (needsLeadingSpace) " $textToInsert" else textToInsert
                prefix + textWithSpace + suffix
            }
            previewText = TextFieldValue(newString, TextRange(newString.length))
            transientHighlight = null
            // Automatically speak out the captured transcription for audible verification
            viewModel.ttsWrapper.speakFeedback(textToInsert)
            viewModel.speechWrapper.clearResults()
        }
    }

    var kActive by remember { mutableStateOf(false) }
    var selActive by remember { mutableStateOf(false) }
    var selAnchor by remember { mutableStateOf<Int?>(null) }
    var idealX by remember { mutableStateOf<Float?>(null) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

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

    // Pulse animation for recording badge and mic button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
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

    val highlightColor = remember(settings.highlightColorHex) { Color(settings.highlightColorHex) }

    fun handleArrow(direction: ArrowDirection) {
        val prevCaret = previewText.selection.end
        val res = CursorLogic.handleArrow(
            value = previewText,
            direction = direction,
            isCharacterMode = kActive,
            isParagraphMode = false,
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

    fun handleDelete() {
        val current = previewText
        val text = current.text
        if (text.isEmpty()) return
        val start = current.selection.min
        val end = current.selection.max
        if (start != end) {
            val newText = text.substring(0, start) + text.substring(end)
            previewText = current.copy(text = newText, selection = TextRange(start, start))
        } else if (start > 0) {
            val newText = text.substring(0, start - 1) + text.substring(start)
            previewText = current.copy(text = newText, selection = TextRange(start - 1, start - 1))
        }
        selActive = false
        selAnchor = null
        idealX = null
        transientHighlight = null
        viewModel.ttsWrapper.speakFeedback("Deleted")
    }

    fun handleEnter() {
        val current = previewText
        val text = current.text
        val start = current.selection.min
        val end = current.selection.max
        val newText = text.substring(0, start) + "\n" + text.substring(end)
        previewText = current.copy(text = newText, selection = TextRange(start + 1, start + 1))
        selActive = false
        selAnchor = null
        idealX = null
        transientHighlight = null
        viewModel.ttsWrapper.speakFeedback("Enter")
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeReplacePopup() },
        containerColor = Color(0xFF141926),
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                    Icon(Icons.Default.Close, contentDescription = "Cancel and Close", tint = Color(0xFF8FA7D8))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sandbox preview text area
            Box(
                modifier = Modifier
                    .weight(1f)
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
                            val isLightHighlight = (highlightColor.red * 0.299f + highlightColor.green * 0.587f + highlightColor.blue * 0.114f) > 0.45f
                            SelectionHighlightTransformation(
                                selection = previewText.selection,
                                transientHighlight = transientHighlight,
                                highlightColor = highlightColor.copy(alpha = 0.7f),
                                highlightedTextColor = if (isLightHighlight) Color(0xFF0D111A) else Color.White
                            )
                        },
                        cursorBrush = SolidColor(Color.Transparent),
                        onTextLayout = { layoutResult = it }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Editing action row: Mode toggle (K), Delete, Enter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // K (Char / Word) Toggle
                Surface(
                    onClick = {
                        kActive = !kActive
                        viewModel.ttsWrapper.speakFeedback(if (kActive) "Character mode" else "Word mode")
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (kActive) Color(0xFF56D0DE) else Color(0xFF1E283C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (kActive) Color(0xFF56D0DE) else Color(0xFF334155)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "K",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (kActive) Color(0xFF0A1926) else Color.White
                        )
                        Text(
                            if (kActive) "CHAR" else "WORD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (kActive) Color(0xFF0A1926) else Color(0xFF8FA7D8)
                        )
                    }
                }

                // Delete Button
                Surface(
                    onClick = { handleDelete() },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E283C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Backspace,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF6B8A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "DEL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8)
                        )
                    }
                }

                // Enter Button
                Surface(
                    onClick = { handleEnter() },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E283C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.KeyboardReturn,
                            contentDescription = "Enter",
                            tint = Color(0xFF56D0DE),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "ENTER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation & Voice Cluster: Mic Button on left + Arrow Cluster on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E131F), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button inside popup (re-dictate / add speech)
                Surface(
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
                    },
                    shape = CircleShape,
                    color = if (isListening) Color(0xFFFF4B6E) else Color(0xFF2563EB),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isListening) Color.White else Color(0xFF93C5FD)
                    ),
                    modifier = Modifier
                        .size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop dictating" else "Dictate more",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Arrow Cluster for popup navigation
                ArrowKeyCluster(
                    selActive = selActive,
                    scale = (settings.arrowSize * 0.85f).coerceIn(0.7f, 1.2f),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons: Cancel and Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.closeReplacePopup() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8FA7D8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { viewModel.applyReplace(previewText.text) },
                    enabled = previewText.text.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Done", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
