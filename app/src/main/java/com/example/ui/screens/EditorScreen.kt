package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ActionButton
import com.example.ui.components.ActionButtonGrid
import com.example.ui.components.ArrowKeyCluster
import com.example.ui.components.SelectionHighlightTransformation
import com.example.ui.components.VoiceMicButton
import com.example.ui.components.instantClickable
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateToSettings: () -> Unit
) {
    val kActive by viewModel.kActive.collectAsState()
    val pActive by viewModel.pActive.collectAsState()
    val selActive by viewModel.selActive.collectAsState()
    val kbLockActive by viewModel.kbLockActive.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val showReplacePopup by viewModel.showReplacePopup.collectAsState()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val transientHighlightRange by viewModel.transientHighlightRange.collectAsState()

    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicClicked()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.loadFromUri(uri)
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) viewModel.saveToUri(uri)
    }

    val defaultOrder = listOf(
        ActionButton.CUT, ActionButton.COPY, ActionButton.K,
        ActionButton.P, ActionButton.DELETE, ActionButton.PASTE, ActionButton.ENTER
    )
    val actualOrder: List<ActionButton> = remember(settings.buttonOrder) {
        val parsed = settings.buttonOrder.split(",").mapNotNull { name ->
            try { ActionButton.valueOf(name.trim()) } catch (_: Exception) { null }
        }
        if (parsed.isNotEmpty()) parsed else defaultOrder
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFECEEF2)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0D10)
                ),
                actions = {
                    com.example.ui.components.ModeToggleButton(
                        label = "KB Lock",
                        isActive = kbLockActive,
                        onClick = viewModel::toggleKbLock,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFFECEEF2)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color(0xFF1B1D22))
                    ) {
                        DropdownMenuItem(
                            text = { Text("New", color = Color(0xFFECEEF2)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.newFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open...", color = Color(0xFFECEEF2)) },
                            onClick = {
                                menuExpanded = false
                                openDocumentLauncher.launch(arrayOf("text/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save", color = Color(0xFFECEEF2)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.saveCurrentFile(onRequireSaveAs = {
                                    createDocumentLauncher.launch(fileName)
                                })
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save As", color = Color(0xFFECEEF2)) },
                            onClick = {
                                menuExpanded = false
                                createDocumentLauncher.launch(fileName)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", color = Color(0xFFECEEF2)) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0D10))
                .padding(padding)
        ) {
            EditorTextArea(
                viewModel = viewModel,
                kbLockActive = kbLockActive,
                transientHighlightRange = transientHighlightRange,
                onTextLayout = { layoutResult.value = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF202228), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1D22))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                ActionButtonGrid(
                    buttonOrder = actualOrder,
                    kActive = kActive,
                    pActive = pActive,
                    onActionClick = viewModel::onAction,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isPlaying by viewModel.ttsWrapper.isPlaying.collectAsState()
                        com.example.ui.components.PlayButton(
                            isPlaying = isPlaying,
                            onClick = viewModel::togglePlay
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Play",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9AA0AC)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VoiceMicButton(
                            isListening = isListening,
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.onMicClicked()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onLongClick = viewModel::openLanguagePicker
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = settings.voiceLanguage,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9AA0AC)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    val baseSize = 42.dp * settings.arrowSize
                    ArrowKeyCluster(
                        selActive = selActive,
                        scale = settings.arrowSize,
                        onMoveUp = { viewModel.moveUp(layoutResult.value) },
                        onMoveDown = { viewModel.moveDown(layoutResult.value) },
                        onMoveLeft = viewModel::moveLeft,
                        onMoveRight = viewModel::moveRight,
                        onToggleSel = viewModel::toggleSel
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .width(baseSize * 0.8f)
                            .height(baseSize * 3 + 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF242730))
                                .instantClickable(onClick = { viewModel.jumpStart() }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Top",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = Color(0xFF9AA0AC),
                                letterSpacing = 0.3.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF242730))
                                .instantClickable(onClick = { viewModel.jumpEnd() }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Bottom",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = Color(0xFF9AA0AC),
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReplacePopup) {
        ReplacePopup(viewModel)
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(viewModel)
    }
}

@Composable
fun EditorTextArea(
    viewModel: EditorViewModel,
    kbLockActive: Boolean,
    transientHighlightRange: TextRange?,
    onTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val textValue by viewModel.textValue.collectAsState()
    var localLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val focusRequester = remember { FocusRequester() }

    val cursorAlpha = remember { Animatable(1f) }
    LaunchedEffect(textValue.selection, textValue.text) {
        cursorAlpha.snapTo(1f)
        while (true) {
            delay(530)
            cursorAlpha.animateTo(0f, animationSpec = tween(120))
            delay(200)
            cursorAlpha.animateTo(1f, animationSpec = tween(120))
        }
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(modifier = modifier) {
        val brightYellowSelection = TextSelectionColors(
            handleColor = Color(0xFFFFD600),
            backgroundColor = Color(0xFFFFD600).copy(alpha = 0.55f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides brightYellowSelection) {
            BasicTextField(
                value = textValue,
                onValueChange = viewModel::onTextChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                readOnly = kbLockActive,
                textStyle = TextStyle(
                    color = Color(0xFFECEEF2),
                    fontSize = 17.sp,
                    lineHeight = 25.5.sp
                ),
                visualTransformation = remember(textValue.selection, transientHighlightRange) {
                    SelectionHighlightTransformation(
                        selection = textValue.selection,
                        transientHighlight = transientHighlightRange,
                        highlightColor = Color(0xFFFFD600).copy(alpha = 0.55f)
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
                "Type something here",
                color = Color(0xFF5A5D66),
                fontSize = 17.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val layout = localLayoutResult
            if (layout != null) {
                // 1. Draw empty-line highlight marker if active range covers an empty or blank line
                val effectiveHlRange = if (textValue.selection.length > 0) {
                    textValue.selection
                } else {
                    transientHighlightRange
                }

                if (effectiveHlRange != null && textValue.text.isNotEmpty()) {
                    val rStart = min(effectiveHlRange.start, effectiveHlRange.end).coerceIn(0, textValue.text.length)
                    val rEnd = max(effectiveHlRange.start, effectiveHlRange.end).coerceIn(0, textValue.text.length)
                    val startLine = layout.getLineForOffset(rStart)
                    val endLine = layout.getLineForOffset(rEnd)

                    for (lineIdx in startLine..endLine) {
                        val lStart = layout.getLineStart(lineIdx)
                        val lEnd = layout.getLineEnd(lineIdx)
                        val lineText = if (lStart < lEnd && lEnd <= textValue.text.length) {
                            textValue.text.substring(lStart, lEnd).trimEnd('\n', '\r')
                        } else ""

                        if (lineText.isEmpty()) {
                            val top = layout.getLineTop(lineIdx)
                            val bottom = layout.getLineBottom(lineIdx)
                            val h = bottom - top
                            drawRoundRect(
                                color = Color(0xFFFFD600).copy(alpha = 0.65f),
                                topLeft = Offset(0f, top + 2.dp.toPx()),
                                size = Size(36.dp.toPx(), (h - 4.dp.toPx()).coerceAtLeast(14.dp.toPx())),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // 2. Draw caret
                val caret = textValue.selection.end.coerceIn(0, textValue.text.length)
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
}
