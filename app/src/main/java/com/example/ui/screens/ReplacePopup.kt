package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
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
import com.example.ui.components.VoiceMicButton
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacePopup(viewModel: EditorViewModel) {
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val finalResult by viewModel.speechWrapper.finalResult.collectAsState()

    var previewText by remember { mutableStateOf(TextFieldValue("")) }
    var transientHighlight by remember { mutableStateOf<TextRange?>(null) }

    LaunchedEffect(finalResult) {
        if (finalResult != null) {
            val textToInsert = finalResult!! + " "
            val start = previewText.selection.min
            val end = previewText.selection.max
            val newString = previewText.text.substring(0, start) + textToInsert + previewText.text.substring(end)
            previewText = TextFieldValue(newString, TextRange(start + textToInsert.length))
            transientHighlight = null
            viewModel.speechWrapper.clearResults()
        }
    }

    var kActive by remember { mutableStateOf(false) }
    var pActive by remember { mutableStateOf(false) }
    var selActive by remember { mutableStateOf(false) }
    var selAnchor by remember { mutableStateOf<Int?>(null) }
    var idealX by remember { mutableStateOf<Float?>(null) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

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

    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isListening) viewModel.speechWrapper.stopListening()
            else viewModel.speechWrapper.startListening(settings.voiceLanguage)
        }
    }

    fun handleArrow(direction: ArrowDirection) {
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
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeReplacePopup() },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Replace Text", style = MaterialTheme.typography.titleLarge)
            Text(
                "Speak or type to create replacement text.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (previewText.text.isEmpty()) {
                    Text("Preview text...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                val brightYellowSelection = TextSelectionColors(
                    handleColor = Color(0xFFFFD600),
                    backgroundColor = Color(0xFFFFD600).copy(alpha = 0.55f)
                )
                CompositionLocalProvider(LocalTextSelectionColors provides brightYellowSelection) {
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp
                        ),
                        visualTransformation = remember(previewText.selection, transientHighlight) {
                            SelectionHighlightTransformation(
                                selection = previewText.selection,
                                transientHighlight = transientHighlight,
                                highlightColor = Color(0xFFFFD600).copy(alpha = 0.55f)
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
                            color = Color(0xFF6C8CFF).copy(alpha = cursorAlpha.value),
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(2.5.dp.toPx(), rect.height),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                ActionButtonGrid(
                    buttonOrder = listOf(
                        ActionButton.CUT, ActionButton.COPY, ActionButton.K,
                        ActionButton.P, ActionButton.DELETE, ActionButton.PASTE, ActionButton.ENTER
                    ),
                    kActive = kActive,
                    pActive = pActive,
                    onActionClick = { action ->
                        when(action) {
                            ActionButton.K -> kActive = !kActive
                            ActionButton.P -> pActive = !pActive
                            else -> {
                                val clipboardText = if (action == ActionButton.PASTE) viewModel.pasteFromClipboard() else null
                                previewText = com.example.logic.TextActionLogic.handleAction(
                                    action = action,
                                    currentValue = previewText,
                                    clipboardText = clipboardText,
                                    onCopy = { viewModel.copyToClipboard(it) }
                                )
                                selActive = false
                                selAnchor = null
                                idealX = null
                                transientHighlight = null
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                VoiceMicButton(
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
                    },
                    onLongClick = {},
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                ArrowKeyCluster(
                    selActive = selActive,
                    scale = settings.arrowSize,
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
                        } else {
                            selAnchor = null
                            val c = previewText.selection.end
                            previewText = previewText.copy(selection = TextRange(c, c))
                            transientHighlight = null
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.closeReplacePopup() }) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.applyReplace(previewText.text) },
                    enabled = previewText.text.isNotEmpty()
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
