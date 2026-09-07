package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
    val selActive by viewModel.selActive.collectAsState()
    val kbLockActive by viewModel.kbLockActive.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val showReplacePopup by viewModel.showReplacePopup.collectAsState()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val isPlaying by viewModel.ttsWrapper.isPlaying.collectAsState()
    val transientHighlightRange by viewModel.transientHighlightRange.collectAsState()

    val searchMatches by viewModel.searchMatches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showMoreControlsSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
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

    val editorBgColor = Color(settings.backgroundColorHex)
    val highlightColor = Color(settings.highlightColorHex)
    val buttonOrderList = remember(settings.buttonOrder) {
        settings.buttonOrder.split(",").filter { it.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (showSearchBar) {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            showSearchBar = false
                            viewModel.clearSearch()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Search",
                                tint = Color(0xFF8FA7D8)
                            )
                        }
                    }
                },
                title = {
                    if (showSearchBar) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search in document...", color = Color(0xFF6B7894), fontSize = 14.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF8FA7D8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = fileName,
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp,
                            color = Color(0xFFD0D7E5)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161A24)
                ),
                actions = {
                    if (showSearchBar) {
                        if (searchQuery.isNotEmpty()) {
                            // Match count badge
                            Surface(
                                color = Color(0xFF20293D),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = if (searchMatches.isNotEmpty()) "${currentMatchIndex + 1}/${searchMatches.size}" else "0/0",
                                    color = if (searchMatches.isNotEmpty()) Color(0xFF56D0DE) else Color(0xFF8FA7D8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Previous Match
                            IconButton(
                                onClick = { viewModel.previousSearchMatch() },
                                enabled = searchMatches.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous Result",
                                    tint = if (searchMatches.isNotEmpty()) Color.White else Color(0xFF4A5568)
                                )
                            }

                            // Next Match
                            IconButton(
                                onClick = { viewModel.nextSearchMatch() },
                                enabled = searchMatches.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next Result",
                                    tint = if (searchMatches.isNotEmpty()) Color.White else Color(0xFF4A5568)
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = {
                            showSearchBar = true
                            if (searchQuery.isNotEmpty()) {
                                viewModel.updateSearchQuery(searchQuery)
                            }
                        }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF8FA7D8)
                            )
                        }

                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = Color(0xFF8FA7D8)
                            )
                        }
                    }
                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(14.dp))
                    ) {
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .width(230.dp)
                                .background(Color(0xFF161E30))
                                .border(1.dp, Color(0xFF26344E), RoundedCornerShape(14.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("New File", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = "New",
                                        tint = Color(0xFF56D0DE),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.newFile()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open...", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Open",
                                        tint = Color(0xFFFFC700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    openDocumentLauncher.launch(arrayOf("text/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Save",
                                        tint = Color(0xFF32D796),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.saveCurrentFile(onRequireSaveAs = {
                                        createDocumentLauncher.launch(fileName)
                                    })
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save As...", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DriveFolderUpload,
                                        contentDescription = "Save As",
                                        tint = Color(0xFF8FA7D8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    createDocumentLauncher.launch(fileName)
                                }
                            )

                            HorizontalDivider(color = Color(0xFF243048), modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Extra Controls (K, P, Lock)", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Controls",
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showMoreControlsSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(editorBgColor)
                .padding(padding)
        ) {
            // Main Text Canvas with Floating READ and MIC buttons
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(editorBgColor)
            ) {
                EditorTextArea(
                    viewModel = viewModel,
                    kbLockActive = kbLockActive,
                    transientHighlightRange = transientHighlightRange,
                    onTextLayout = { layoutResult.value = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                // Floating Action Bar on bottom-right of editor canvas
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // READ Button (Dark container with play/speaker icon + "READ" label)
                    FloatingReadButton(
                        isPlaying = isPlaying,
                        onClick = viewModel::togglePlay
                    )

                    // MIC Button (Bright Cyan pill with black mic icon)
                    FloatingMicButton(
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
                }
            }

            // Bottom Keypad / Control Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0E18))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: 2x3 Grid (Customizable buttons)
                    ActionButtonGrid(
                        onActionClick = viewModel::onAction,
                        onMoreClick = { showMoreControlsSheet = true },
                        buttonOrder = buttonOrderList,
                        sizeMultiplier = settings.buttonSizeMultiplier,
                        modifier = Modifier.weight(1f)
                    )

                    // Right Side: D-Pad Directional Cluster
                    ArrowKeyCluster(
                        selActive = selActive,
                        scale = settings.arrowSize,
                        onMoveUp = { viewModel.moveUp(layoutResult.value) },
                        onMoveDown = { viewModel.moveDown(layoutResult.value) },
                        onMoveLeft = viewModel::moveLeft,
                        onMoveRight = viewModel::moveRight,
                        onToggleSel = viewModel::toggleSel,
                        activeHighlightColor = highlightColor,
                        modifier = Modifier.wrapContentWidth()
                    )
                }
            }
        }
    }

    if (showMoreControlsSheet) {
        MoreControlsSheet(
            viewModel = viewModel,
            onNavigateToSettings = onNavigateToSettings,
            onDismiss = { showMoreControlsSheet = false }
        )
    }

    if (showReplacePopup) {
        ReplacePopup(viewModel)
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(viewModel)
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
            .instantClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isListening) Color(0xFFFF4B6E) else Color(0xFF56D0DE)

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
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
    val settings by viewModel.settings.collectAsState()
    var localLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val focusRequester = remember { FocusRequester() }

    val textColor = Color(settings.textColorHex)
    val highlightColor = Color(settings.highlightColorHex)
    val textSize = settings.textSizeSp.sp
    val lineHeight = (settings.textSizeSp * 1.45f).sp

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

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(kbLockActive) {
        if (kbLockActive) {
            keyboardController?.hide()
        }
    }

    Box(modifier = modifier) {
        val brightSelectionColors = TextSelectionColors(
            handleColor = highlightColor,
            backgroundColor = highlightColor.copy(alpha = 0.55f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides brightSelectionColors) {
            BasicTextField(
                value = textValue,
                onValueChange = viewModel::onTextChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                readOnly = kbLockActive,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = textSize,
                    lineHeight = lineHeight
                ),
                visualTransformation = remember(textValue.selection, transientHighlightRange, highlightColor) {
                    SelectionHighlightTransformation(
                        selection = textValue.selection,
                        transientHighlight = transientHighlightRange,
                        highlightColor = highlightColor.copy(alpha = 0.55f)
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
                                color = highlightColor.copy(alpha = 0.65f),
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
                    color = highlightColor.copy(alpha = cursorAlpha.value),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(2.5.dp.toPx(), rect.height),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }
    }
}
