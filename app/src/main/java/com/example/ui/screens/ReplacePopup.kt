package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.editor.dialogs.ReplaceActionRow
import com.example.core.editor.dialogs.ReplaceDialogButtons
import com.example.core.editor.dialogs.ReplaceNavigationCluster
import com.example.core.editor.dialogs.ReplacePreviewBox
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.presentation.editor.EditorViewModel
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
            ReplacePreviewBox(
                previewText = previewText,
                onPreviewTextChange = {
                    previewText = it
                    if (selActive) selActive = false
                    selAnchor = null
                    idealX = null
                    transientHighlight = null
                },
                isListening = isListening,
                partialResults = partialResults,
                highlightColor = highlightColor,
                cursorAlpha = cursorAlpha,
                transientHighlight = transientHighlight,
                layoutResult = layoutResult,
                onTextLayout = { layoutResult = it },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Editing action row: Mode toggle (K), Delete, Enter
            ReplaceActionRow(
                kActive = kActive,
                onToggleK = {
                    kActive = !kActive
                    viewModel.ttsWrapper.speakFeedback(if (kActive) "Character mode" else "Word mode")
                },
                onDelete = { handleDelete() },
                onEnter = { handleEnter() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation & Voice Cluster: Mic Button on left + Arrow Cluster on right
            ReplaceNavigationCluster(
                isListening = isListening,
                onMicClick = {
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
                selActive = selActive,
                arrowScale = (settings.arrowSize * 0.85f).coerceIn(0.7f, 1.2f),
                highlightColor = highlightColor,
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
                modifier = Modifier.background(Color(0xFF0E131F), RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons: Cancel and Done
            ReplaceDialogButtons(
                onCancel = { viewModel.closeReplacePopup() },
                onApply = { viewModel.applyReplace(previewText.text) },
                canApply = previewText.text.isNotEmpty()
            )
        }
    }
}
