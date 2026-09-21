package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.logic.CursorLogic
import com.example.presentation.editor.EditorViewModel
import com.example.ui.components.SelectionHighlightTransformation

private val ReadingEmptyTextToolbar = object : TextToolbar {
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

private val InvisibleSelectionColors = TextSelectionColors(
    handleColor = Color.Transparent,
    backgroundColor = Color.Transparent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingModeScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel
) {
    val context = LocalContext.current
    val textValue by viewModel.textValue.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isPlaying by viewModel.ttsWrapper.isPlaying.collectAsState()
    val speechHighlightRange by viewModel.speechHighlightRange.collectAsState()
    val showReplacePopup by viewModel.showReplacePopup.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    // Stop playback when leaving Reading Mode
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    var isParagraphModeActive by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var editSubMenuExpanded by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }

    // Long press custom popup state
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuTouchOffset by remember { mutableStateOf(Offset.Zero) }
    var contextMenuSelectedRange by remember { mutableStateOf<TextRange?>(null) }

    var localLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()

    val highlightColor = remember(settings.highlightColorHex) {
        Color(settings.highlightColorHex)
    }

    val cachedOffsetMap = remember(textValue.text, settings.hideHeadingSymbols) {
        if (settings.hideHeadingSymbols && textValue.text.isNotEmpty()) {
            SelectionHighlightTransformation.computeOffsetMap(textValue.text)
        } else {
            null
        }
    }

    val visualTransformation = remember(
        textValue.selection,
        speechHighlightRange,
        highlightColor,
        settings.hideHeadingSymbols,
        cachedOffsetMap
    ) {
        SelectionHighlightTransformation(
            selection = textValue.selection,
            speechHighlight = speechHighlightRange,
            highlightColor = highlightColor,
            speechHighlightColor = highlightColor, // Consistent bright yellow
            highlightedTextColor = Color(0xFF090D16),
            hideHeadingSymbols = settings.hideHeadingSymbols,
            cachedMapping = cachedOffsetMap
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reading_mode_screen"),
        containerColor = Color(0xFF0C0E14),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reading Mode",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFECEEF2)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.stopPlayback()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("reading_mode_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Reading Mode",
                            tint = Color(0xFF8FA7D8)
                        )
                    }
                },
                actions = {
                    // Text size button
                    Box {
                        IconButton(
                            onClick = { showTextSizeDialog = !showTextSizeDialog },
                            modifier = Modifier.testTag("reading_mode_text_size_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Adjust Text Size",
                                tint = if (showTextSizeDialog) Color(0xFF56D0DE) else Color(0xFF8FA7D8)
                            )
                        }

                        // Text size adjustment popup
                        if (showTextSizeDialog) {
                            Popup(
                                alignment = Alignment.TopEnd,
                                offset = IntOffset(0, 120),
                                onDismissRequest = { showTextSizeDialog = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF161E30),
                                    border = BorderStroke(1.5.dp, Color(0xFF26344E)),
                                    shadowElevation = 12.dp,
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Text Size",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8FA7D8)
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val newSize = (settings.textSizeSp - 2f).coerceAtLeast(12f)
                                                    viewModel.updateTextSize(newSize)
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(Color(0xFF1E283E), RoundedCornerShape(10.dp))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Decrease Font Size",
                                                    tint = Color(0xFFECEEF2)
                                                )
                                            }

                                            Text(
                                                text = "${settings.textSizeSp.toInt()} sp",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF56D0DE),
                                                modifier = Modifier.widthIn(min = 54.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    val newSize = (settings.textSizeSp + 2f).coerceAtMost(48f)
                                                    viewModel.updateTextSize(newSize)
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(Color(0xFF1E283E), RoundedCornerShape(10.dp))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Increase Font Size",
                                                    tint = Color(0xFFECEEF2)
                                                )
                                            }
                                        }

                                        // Preset sizes
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(16f, 20f, 24f, 28f, 32f).forEach { size ->
                                                val isSelected = settings.textSizeSp.toInt() == size.toInt()
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isSelected) Color(0xFF2563EB) else Color(0xFF1E283E),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { viewModel.updateTextSize(size) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${size.toInt()}",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else Color(0xFF8FA7D8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Three-dot menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = !menuExpanded },
                            modifier = Modifier.testTag("reading_mode_three_dot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = Color(0xFF8FA7D8)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                                editSubMenuExpanded = false
                            },
                            modifier = Modifier.background(Color(0xFF161E30))
                        ) {
                            // Play from cursor
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Play from cursor",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play from cursor",
                                        tint = Color(0xFF32D796)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.togglePlay()
                                }
                            )

                            // Play from beginning
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Play from beginning",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Play from beginning",
                                        tint = Color(0xFF56D0DE)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.playReadingModeFromTop()
                                }
                            )

                            // Paragraph mode toggle
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isParagraphModeActive) "Paragraph Mode: ON" else "Paragraph Mode: OFF",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isParagraphModeActive) Color(0xFF56D0DE) else Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Segment,
                                        contentDescription = "Paragraph Mode Toggle",
                                        tint = if (isParagraphModeActive) Color(0xFF56D0DE) else Color(0xFF8FA7D8)
                                    )
                                },
                                trailingIcon = {
                                    if (isParagraphModeActive) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = Color(0xFF56D0DE),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    isParagraphModeActive = !isParagraphModeActive
                                    if (isParagraphModeActive) {
                                        viewModel.ttsWrapper.speakFeedback("Paragraph mode active. Tap any paragraph to read.")
                                    } else {
                                        viewModel.ttsWrapper.speakFeedback("Paragraph mode off")
                                    }
                                    menuExpanded = false
                                }
                            )

                            HorizontalDivider(color = Color(0xFF26344E))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Edit",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Options",
                                        tint = Color(0xFF56D0DE)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (editSubMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF8FA7D8)
                                    )
                                },
                                onClick = {
                                    editSubMenuExpanded = !editSubMenuExpanded
                                }
                            )

                            if (editSubMenuExpanded) {
                                HorizontalDivider(color = Color(0xFF26344E))

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Undo",
                                            color = if (canUndo) Color(0xFFECEEF2) else Color(0xFF4A5568)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo",
                                            tint = if (canUndo) Color(0xFF56D0DE) else Color(0xFF4A5568)
                                        )
                                    },
                                    enabled = canUndo,
                                    onClick = {
                                        viewModel.undo()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Redo",
                                            color = if (canRedo) Color(0xFFECEEF2) else Color(0xFF4A5568)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Redo,
                                            contentDescription = "Redo",
                                            tint = if (canRedo) Color(0xFF56D0DE) else Color(0xFF4A5568)
                                        )
                                    },
                                    enabled = canRedo,
                                    onClick = {
                                        viewModel.redo()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Add new line",
                                            color = Color(0xFFECEEF2)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardReturn,
                                            contentDescription = "Add new line",
                                            tint = Color(0xFF32D796)
                                        )
                                    },
                                    onClick = {
                                        viewModel.addNewLine()
                                        menuExpanded = false
                                        editSubMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0E14)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Text Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                CompositionLocalProvider(
                    LocalTextSelectionColors provides InvisibleSelectionColors,
                    LocalTextToolbar provides ReadingEmptyTextToolbar
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 120.dp)
                            .testTag("reading_mode_text_canvas"),
                        textStyle = TextStyle(
                            color = Color(0xFFECEEF2),
                            fontSize = settings.textSizeSp.sp,
                            lineHeight = (settings.textSizeSp * 1.55f).sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color.Transparent),
                        visualTransformation = visualTransformation,
                        onTextLayout = { localLayoutResult = it }
                    )
                }

                // Transparent overlay to reliably capture taps and long-presses over the text
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(isParagraphModeActive, textValue.text, settings.hideHeadingSymbols) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    if (showContextMenu) {
                                        showContextMenu = false
                                        viewModel.clearSelection()
                                        return@detectTapGestures
                                    }

                                    localLayoutResult?.let { layout ->
                                        val transOffset = layout.getOffsetForPosition(tapOffset)
                                        val origOffset = SelectionHighlightTransformation.transformedToOriginal(
                                            textValue.text,
                                            transOffset,
                                            settings.hideHeadingSymbols
                                        )

                                        if (isParagraphModeActive) {
                                            val paraRange = findParagraphRange(textValue.text, origOffset)
                                            viewModel.playFrom(paraRange.start, paraRange.end)
                                        } else {
                                            viewModel.setCaretFromTap(origOffset)
                                        }
                                    }
                                },
                                onLongPress = { longPressOffset ->
                                    localLayoutResult?.let { layout ->
                                        val transOffset = layout.getOffsetForPosition(longPressOffset)
                                        val origOffset = SelectionHighlightTransformation.transformedToOriginal(
                                            textValue.text,
                                            transOffset,
                                            settings.hideHeadingSymbols
                                        )

                                        val wordRange = CursorLogic.getWordRangeAt(textValue.text, origOffset).let {
                                            if (it.start == it.end && textValue.text.isNotEmpty()) {
                                                val s = origOffset.coerceIn(0, textValue.text.length)
                                                val e = (origOffset + 1).coerceIn(0, textValue.text.length)
                                                TextRange(s, e)
                                            } else it
                                        }

                                        if (wordRange.length > 0) {
                                            viewModel.setSelectionRange(wordRange)
                                            contextMenuTouchOffset = longPressOffset
                                            contextMenuSelectedRange = wordRange
                                            showContextMenu = true
                                        }
                                    }
                                }
                            )
                        }
                )
            }

            // Custom Long-Press Context Menu Popup
            if (showContextMenu && contextMenuSelectedRange != null && contextMenuSelectedRange!!.length > 0) {
                val density = LocalDensity.current

                Popup(
                    alignment = Alignment.TopCenter,
                    offset = with(density) {
                        val yOffset = (contextMenuTouchOffset.y - scrollState.value - 64.dp.toPx()).toInt().coerceAtLeast(10)
                        IntOffset(0, yOffset)
                    },
                    onDismissRequest = {
                        showContextMenu = false
                        viewModel.clearSelection()
                    },
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF161E30),
                        border = BorderStroke(1.5.dp, Color(0xFF26344E)),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("reading_mode_context_menu")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Copy
                            ContextMenuItem(
                                icon = Icons.Default.ContentCopy,
                                label = "Copy",
                                tint = Color(0xFF56D0DE),
                                onClick = {
                                    val range = contextMenuSelectedRange
                                    if (range != null && range.length > 0) {
                                        val subText = textValue.text.substring(range.min, range.max)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("text", subText)
                                        clipboard.setPrimaryClip(clip)
                                        viewModel.ttsWrapper.speakFeedback("Copied")
                                    }
                                    showContextMenu = false
                                    viewModel.clearSelection()
                                }
                            )

                            VerticalDivider(
                                color = Color(0xFF26344E),
                                modifier = Modifier.height(28.dp)
                            )

                            // Read
                            ContextMenuItem(
                                icon = Icons.Default.VolumeUp,
                                label = "Read",
                                tint = Color(0xFF32D796),
                                onClick = {
                                    val start = contextMenuSelectedRange?.min ?: 0
                                    showContextMenu = false
                                    viewModel.playFrom(start)
                                }
                            )

                            VerticalDivider(
                                color = Color(0xFF26344E),
                                modifier = Modifier.height(28.dp)
                            )

                            // Edit (Voice Replace)
                            ContextMenuItem(
                                icon = Icons.Default.Mic,
                                label = "Edit",
                                tint = Color(0xFFFFC700),
                                onClick = {
                                    showContextMenu = false
                                    viewModel.openVoiceReplacePopup()
                                }
                            )

                            VerticalDivider(
                                color = Color(0xFF26344E),
                                modifier = Modifier.height(28.dp)
                            )

                            // Delete
                            ContextMenuItem(
                                icon = Icons.Default.DeleteOutline,
                                label = "Delete",
                                tint = Color(0xFFFF6584),
                                onClick = {
                                    showContextMenu = false
                                    viewModel.deleteSelection()
                                    viewModel.ttsWrapper.speakFeedback("Deleted")
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Minimal Reading Controls
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .wrapContentWidth(),
                shape = RoundedCornerShape(36.dp),
                color = Color(0xFF141A28),
                border = BorderStroke(1.5.dp, Color(0xFF26344E)),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Button
                    Surface(
                        onClick = {
                            if (isPlaying) {
                                viewModel.stopPlayback()
                            } else {
                                viewModel.playReadingModeFromTop()
                            }
                        },
                        shape = CircleShape,
                        color = if (isPlaying) Color(0xFFFF4D6D) else Color(0xFF2563EB),
                        border = BorderStroke(
                            2.dp,
                            if (isPlaying) Color(0xFFFF8DA1) else Color(0xFF60A5FA)
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("reading_mode_play_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop Reading" else "Read from Top",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Paragraph Toggle Button
                    Surface(
                        onClick = {
                            isParagraphModeActive = !isParagraphModeActive
                            if (isParagraphModeActive) {
                                viewModel.ttsWrapper.speakFeedback("Paragraph mode active. Tap any paragraph to read.")
                            } else {
                                viewModel.ttsWrapper.speakFeedback("Paragraph mode off")
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        color = if (isParagraphModeActive) Color(0xFF56D0DE).copy(alpha = 0.22f) else Color(0xFF1E2638),
                        border = BorderStroke(
                            1.5.dp,
                            if (isParagraphModeActive) Color(0xFF56D0DE) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("reading_mode_paragraph_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Segment,
                                contentDescription = "Paragraph Mode Toggle",
                                tint = if (isParagraphModeActive) Color(0xFF56D0DE) else Color(0xFF8FA7D8),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isParagraphModeActive) "Paragraph: ON" else "Paragraph: OFF",
                                color = if (isParagraphModeActive) Color(0xFF56D0DE) else Color(0xFF8FA7D8),
                                fontSize = 14.sp,
                                fontWeight = if (isParagraphModeActive) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Voice Replace Popup reuse
            if (showReplacePopup) {
                ReplacePopup(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFECEEF2)
            )
        }
    }
}

private fun findParagraphRange(text: String, offset: Int): TextRange {
    if (text.isEmpty()) return TextRange(0, 0)
    val clamped = offset.coerceIn(0, text.length)
    val start = if (clamped == 0) 0 else {
        val prevNl = text.lastIndexOf('\n', (clamped - 1).coerceAtLeast(0))
        if (prevNl == -1) 0 else prevNl + 1
    }
    val nextNl = text.indexOf('\n', clamped)
    val end = if (nextNl == -1) text.length else nextNl
    return TextRange(start, maxOf(start, end))
}
