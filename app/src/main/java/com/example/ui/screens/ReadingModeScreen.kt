package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import com.example.ui.components.SelectionDragHandles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.TextFieldValue
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

enum class ReadingFunction(
    val id: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    PARAGRAPH(
        id = "paragraph",
        title = "Paragraph Read",
        shortLabel = "Paragraph",
        description = "Tap any paragraph to read it aloud",
        icon = Icons.Default.Segment
    ),
    READ_FROM_TOP(
        id = "from_top",
        title = "Read from Top",
        shortLabel = "From Top",
        description = "Start reading whole text from beginning",
        icon = Icons.Default.VolumeUp
    ),
    PLAY_FROM_CURSOR(
        id = "from_cursor",
        title = "Play from Cursor",
        shortLabel = "From Cursor",
        description = "Start reading from current cursor position",
        icon = Icons.Default.PlayArrow
    ),
    SENTENCE_READ(
        id = "sentence",
        title = "Sentence Read",
        shortLabel = "Sentence",
        description = "Read the current sentence aloud",
        icon = Icons.Default.FormatQuote
    ),
    CHARACTER_READ(
        id = "character",
        title = "Character Read",
        shortLabel = "Char Read",
        description = "Read character-by-character from cursor",
        icon = Icons.Default.Spellcheck
    )
}

fun findSentenceRange(text: String, offset: Int): TextRange {
    if (text.isEmpty()) return TextRange(0, 0)
    val clamped = offset.coerceIn(0, text.length)
    val delimiters = charArrayOf('.', '!', '?', '\n')
    var start = 0
    for (i in (clamped - 1) downTo 0) {
        if (text[i] in delimiters) {
            start = i + 1
            while (start < text.length && text[start].isWhitespace()) start++
            break
        }
    }
    var end = text.length
    for (i in clamped until text.length) {
        if (text[i] in delimiters) {
            end = i + 1
            break
        }
    }
    val s = start.coerceIn(0, text.length)
    val e = end.coerceIn(s, text.length)
    return TextRange(s, e)
}

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
    var selectedFunction by remember { mutableStateOf(ReadingFunction.PARAGRAPH) }
    var showFunctionPickerDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    // Long press custom popup state
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuTouchOffset by remember { mutableStateOf(Offset.Zero) }
    var contextMenuSelectedRange by remember { mutableStateOf<TextRange?>(null) }

    // Standalone reading selection state completely decoupled from Home (Editor) screen selection
    var readingSelection by remember { mutableStateOf(TextRange.Zero) }

    var localLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()

    val highlightColor = remember(settings.highlightColorHex) {
        Color(settings.highlightColorHex)
    }

    val isLightHighlight = remember(highlightColor) {
        (highlightColor.red * 0.299f + highlightColor.green * 0.587f + highlightColor.blue * 0.114f) > 0.45f
    }

    val cachedOffsetMap = remember(textValue.text, settings.hideHeadingSymbols) {
        if (settings.hideHeadingSymbols && textValue.text.isNotEmpty()) {
            SelectionHighlightTransformation.computeOffsetMap(textValue.text)
        } else {
            null
        }
    }

    val visualTransformation = remember(
        readingSelection,
        speechHighlightRange,
        highlightColor,
        isLightHighlight,
        settings.hideHeadingSymbols,
        cachedOffsetMap
    ) {
        SelectionHighlightTransformation(
            selection = readingSelection,
            speechHighlight = speechHighlightRange,
            highlightColor = highlightColor.copy(alpha = 0.45f),
            speechHighlightColor = highlightColor,
            highlightedTextColor = if (isLightHighlight) Color(0xFF090D16) else Color.White,
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
                    // Three-dot menu containing all options
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
                            },
                            modifier = Modifier.background(Color(0xFF161E30))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Copy all text",
                                        color = Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy all text",
                                        tint = Color(0xFF56D0DE)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("text", textValue.text)
                                    clipboard.setPrimaryClip(clip)
                                    viewModel.ttsWrapper.speakFeedback("Entire text copied")
                                }
                            )

                            HorizontalDivider(color = Color(0xFF26344E))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Text size",
                                        color = Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Text size",
                                        tint = Color(0xFF56D0DE)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showTextSizeDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Highlight color",
                                        color = Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Highlight color",
                                        tint = highlightColor
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showColorPickerDialog = true
                                }
                            )

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
                                    menuExpanded = false
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
                                    menuExpanded = false
                                    viewModel.redo()
                                }
                            )

                            HorizontalDivider(color = Color(0xFF26344E))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Select all",
                                        color = Color(0xFFECEEF2)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SelectAll,
                                        contentDescription = "Select all",
                                        tint = Color(0xFF56D0DE)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    val allRange = TextRange(0, textValue.text.length)
                                    readingSelection = allRange
                                    contextMenuSelectedRange = allRange
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
                                    menuExpanded = false
                                    viewModel.addNewLine()
                                }
                            )
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
                val readingTextFieldValue = remember(textValue.text, readingSelection) {
                    TextFieldValue(text = textValue.text, selection = readingSelection)
                }

                CompositionLocalProvider(
                    LocalTextSelectionColors provides InvisibleSelectionColors,
                    LocalTextToolbar provides ReadingEmptyTextToolbar
                ) {
                    BasicTextField(
                        value = readingTextFieldValue,
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
                        .pointerInput(isParagraphModeActive, textValue.text, settings.hideHeadingSymbols, cachedOffsetMap, readingSelection) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    // If selection is active, check if tap was near handles so we NEVER clear selection
                                    if (readingSelection.length > 0 && localLayoutResult != null) {
                                        val layout = localLayoutResult!!
                                        try {
                                            val tLen = layout.layoutInput.text.length
                                            val tStart = if (settings.hideHeadingSymbols && cachedOffsetMap != null) {
                                                SelectionHighlightTransformation.originalToTransformed(readingSelection.min, cachedOffsetMap)
                                            } else {
                                                SelectionHighlightTransformation.originalToTransformed(textValue.text, readingSelection.min, settings.hideHeadingSymbols)
                                            }.coerceIn(0, tLen)
                                            val tEnd = if (settings.hideHeadingSymbols && cachedOffsetMap != null) {
                                                SelectionHighlightTransformation.originalToTransformed(readingSelection.max, cachedOffsetMap)
                                            } else {
                                                SelectionHighlightTransformation.originalToTransformed(textValue.text, readingSelection.max, settings.hideHeadingSymbols)
                                            }.coerceIn(0, tLen)

                                            val sRect = layout.getCursorRect(tStart)
                                            val eRect = layout.getCursorRect(tEnd)
                                            val knobRadius = 12.dp.toPx()
                                            val hitDist = 56.dp.toPx()

                                            val distS = (tapOffset - Offset(sRect.left, sRect.bottom + knobRadius)).getDistance()
                                            val distE = (tapOffset - Offset(eRect.right, eRect.bottom + knobRadius)).getDistance()
                                            if (distS <= hitDist || distE <= hitDist) {
                                                // Tapped on or near handle, do nothing to preserve selection
                                                return@detectTapGestures
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    if (showContextMenu) {
                                        showContextMenu = false
                                    }

                                    localLayoutResult?.let { layout ->
                                        val transOffset = layout.getOffsetForPosition(tapOffset)
                                        val origOffset = SelectionHighlightTransformation.transformedToOriginal(
                                            transOffset,
                                            cachedOffsetMap
                                        )

                                        if (isParagraphModeActive) {
                                            val paraRange = findParagraphRange(textValue.text, origOffset)
                                            viewModel.playFrom(paraRange.start, paraRange.end)
                                        } else {
                                            // Decoupled cursor position in Reading Mode - does not change Home Screen selection
                                            readingSelection = TextRange(origOffset, origOffset)
                                        }
                                    }
                                },
                                onLongPress = { longPressOffset ->
                                    localLayoutResult?.let { layout ->
                                        val transOffset = layout.getOffsetForPosition(longPressOffset)
                                        val origOffset = SelectionHighlightTransformation.transformedToOriginal(
                                            transOffset,
                                            cachedOffsetMap
                                        )

                                        val wordRange = CursorLogic.getWordRangeAt(textValue.text, origOffset).let {
                                            if (it.start == it.end && textValue.text.isNotEmpty()) {
                                                val s = origOffset.coerceIn(0, textValue.text.length)
                                                val e = (origOffset + 1).coerceIn(0, textValue.text.length)
                                                TextRange(s, e)
                                            } else it
                                        }

                                        if (wordRange.length > 0) {
                                            // Standalone selection in Reading Mode
                                            readingSelection = wordRange
                                            contextMenuTouchOffset = longPressOffset
                                            contextMenuSelectedRange = wordRange
                                            showContextMenu = true
                                        }
                                    }
                                }
                            )
                        }
                )

                // Selection Drag Handles for adjusting text selection
                if (readingSelection.length > 0) {
                    SelectionDragHandles(
                        text = textValue.text,
                        selection = readingSelection,
                        layoutResult = localLayoutResult,
                        hideHeadingSymbols = settings.hideHeadingSymbols,
                        highlightColor = highlightColor,
                        cachedMapping = cachedOffsetMap,
                        modifier = Modifier.matchParentSize(),
                        onSelectionChange = { newRange ->
                            readingSelection = newRange
                            contextMenuSelectedRange = newRange
                            localLayoutResult?.let { layout ->
                                try {
                                    val transEnd = if (settings.hideHeadingSymbols && cachedOffsetMap != null) {
                                        SelectionHighlightTransformation.originalToTransformed(newRange.max, cachedOffsetMap)
                                    } else {
                                        SelectionHighlightTransformation.originalToTransformed(textValue.text, newRange.max, settings.hideHeadingSymbols)
                                    }.coerceIn(0, layout.layoutInput.text.length)
                                    val rect = layout.getCursorRect(transEnd)
                                    contextMenuTouchOffset = Offset(rect.right, rect.top)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
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
                    },
                    properties = PopupProperties(
                        focusable = false,
                        dismissOnClickOutside = false
                    )
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
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
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
                                        readingSelection = TextRange.Zero
                                        contextMenuSelectedRange = null
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
                                        val end = contextMenuSelectedRange?.max
                                        showContextMenu = false
                                        readingSelection = TextRange.Zero
                                        contextMenuSelectedRange = null
                                        viewModel.playFrom(start, end)
                                    }
                                )

                                VerticalDivider(
                                    color = Color(0xFF26344E),
                                    modifier = Modifier.height(28.dp)
                                )

                                // Edit (Voice & Text Replace)
                                ContextMenuItem(
                                    icon = Icons.Default.Mic,
                                    label = "Edit",
                                    tint = Color(0xFF38BDF8),
                                    onClick = {
                                        showContextMenu = false
                                        readingSelection = TextRange.Zero
                                        contextMenuSelectedRange = null
                                        viewModel.openVoiceReplacePopup()
                                    }
                                )

                                VerticalDivider(
                                    color = Color(0xFF26344E),
                                    modifier = Modifier.height(28.dp)
                                )

                                // Delete (Delete selected text)
                                ContextMenuItem(
                                    icon = Icons.Default.DeleteOutline,
                                    label = "Delete",
                                    tint = Color(0xFFFF6584),
                                    onClick = {
                                        val range = contextMenuSelectedRange
                                        if (range != null && range.length > 0) {
                                            viewModel.deleteRange(range)
                                            viewModel.ttsWrapper.speakFeedback("Deleted")
                                        }
                                        showContextMenu = false
                                        readingSelection = TextRange.Zero
                                        contextMenuSelectedRange = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Right Minimal Reading Controls (Small Buttons)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
                    .wrapContentSize(),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF141A28).copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, Color(0xFF26344E)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Customizable Function Button (Long-press to customize function)
                    val isFuncActive = when (selectedFunction) {
                        ReadingFunction.PARAGRAPH -> isParagraphModeActive
                        else -> isPlaying
                    }
                    val buttonLabel = when (selectedFunction) {
                        ReadingFunction.PARAGRAPH -> if (isParagraphModeActive) "Para: ON" else "Para: OFF"
                        else -> selectedFunction.shortLabel
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isFuncActive) Color(0xFF56D0DE).copy(alpha = 0.22f) else Color(0xFF1E2638),
                        border = BorderStroke(
                            1.5.dp,
                            if (isFuncActive) Color(0xFF56D0DE) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("reading_mode_custom_action_button")
                            .pointerInput(selectedFunction, isParagraphModeActive, textValue.text, isPlaying, readingSelection) {
                                detectTapGestures(
                                    onLongPress = {
                                        showFunctionPickerDialog = true
                                    },
                                    onTap = {
                                        when (selectedFunction) {
                                            ReadingFunction.PARAGRAPH -> {
                                                isParagraphModeActive = !isParagraphModeActive
                                                readingSelection = TextRange.Zero
                                                contextMenuSelectedRange = null
                                                showContextMenu = false
                                                if (isParagraphModeActive) {
                                                    viewModel.ttsWrapper.speakFeedback("Paragraph mode on. Tap any paragraph to read.")
                                                } else {
                                                    viewModel.ttsWrapper.speakFeedback("Paragraph mode off")
                                                }
                                            }
                                            ReadingFunction.READ_FROM_TOP -> {
                                                readingSelection = TextRange.Zero
                                                contextMenuSelectedRange = null
                                                showContextMenu = false
                                                viewModel.playReadingModeFromTop()
                                            }
                                            ReadingFunction.PLAY_FROM_CURSOR -> {
                                                val start = readingSelection.min.coerceIn(0, textValue.text.length)
                                                readingSelection = TextRange.Zero
                                                contextMenuSelectedRange = null
                                                showContextMenu = false
                                                viewModel.playFrom(start)
                                            }
                                            ReadingFunction.SENTENCE_READ -> {
                                                val range = findSentenceRange(textValue.text, readingSelection.min)
                                                readingSelection = TextRange.Zero
                                                contextMenuSelectedRange = null
                                                showContextMenu = false
                                                if (range.length > 0) {
                                                    viewModel.playFrom(range.start, range.end)
                                                } else {
                                                    viewModel.ttsWrapper.speakFeedback("No sentence found")
                                                }
                                            }
                                            ReadingFunction.CHARACTER_READ -> {
                                                val cur = readingSelection.min.coerceIn(0, textValue.text.length)
                                                if (cur < textValue.text.length) {
                                                    val ch = textValue.text[cur].toString()
                                                    val desc = when (ch) {
                                                        " " -> "Space"
                                                        "\n" -> "New line"
                                                        "\t" -> "Tab"
                                                        else -> ch
                                                    }
                                                    viewModel.ttsWrapper.speakFeedback(desc)
                                                    val next = (cur + 1).coerceAtMost(textValue.text.length)
                                                    readingSelection = TextRange(next, next)
                                                } else {
                                                    viewModel.ttsWrapper.speakFeedback("End of text")
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = selectedFunction.icon,
                                contentDescription = selectedFunction.title,
                                tint = if (isFuncActive) Color(0xFF56D0DE) else Color(0xFF8FA7D8),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = buttonLabel,
                                color = if (isFuncActive) Color(0xFF56D0DE) else Color(0xFFECEEF2),
                                fontSize = 13.sp,
                                fontWeight = if (isFuncActive) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Play / Stop Button (Small button in bottom right)
                    Surface(
                        onClick = {
                            if (isPlaying) {
                                viewModel.stopPlayback()
                            } else {
                                val currentSel = readingSelection
                                readingSelection = TextRange.Zero
                                contextMenuSelectedRange = null
                                showContextMenu = false
                                when (selectedFunction) {
                                    ReadingFunction.READ_FROM_TOP -> viewModel.playReadingModeFromTop()
                                    else -> viewModel.playReadingSelection(currentSel)
                                }
                            }
                        },
                        shape = CircleShape,
                        color = if (isPlaying) Color(0xFFFF4D6D) else Color(0xFF2563EB),
                        border = BorderStroke(
                            1.5.dp,
                            if (isPlaying) Color(0xFFFF8DA1) else Color(0xFF60A5FA)
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("reading_mode_play_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop Reading" else "Read",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Function Picker Dialog for Long Pressing the Quick Button
            if (showFunctionPickerDialog) {
                AlertDialog(
                    onDismissRequest = { showFunctionPickerDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF56D0DE)
                            )
                            Text(
                                text = "Assign Button Function",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFECEEF2)
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Highlighting Mode switcher (Line vs Word)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E283E),
                                border = BorderStroke(1.dp, Color(0xFF2E3A52)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Highlight Mode",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFECEEF2)
                                            )
                                            Text(
                                                text = if (settings.highlightUnit == "LINE") "Line-by-line (Fast & efficient)" else "Word-by-word",
                                                fontSize = 12.sp,
                                                color = Color(0xFF8FA7D8)
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val isLine = settings.highlightUnit == "LINE"
                                            Surface(
                                                onClick = {
                                                    viewModel.updateHighlightUnit("LINE")
                                                    viewModel.ttsWrapper.speakFeedback("Line highlight mode active")
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isLine) Color(0xFF2563EB) else Color(0xFF161E30),
                                                border = BorderStroke(1.dp, if (isLine) Color(0xFF56D0DE) else Color(0xFF2E3A52))
                                            ) {
                                                Text(
                                                    text = "Line",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isLine) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isLine) Color.White else Color(0xFF8FA7D8),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }

                                            Surface(
                                                onClick = {
                                                    viewModel.updateHighlightUnit("WORD")
                                                    viewModel.ttsWrapper.speakFeedback("Word highlight mode active")
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (!isLine) Color(0xFF2563EB) else Color(0xFF161E30),
                                                border = BorderStroke(1.dp, if (!isLine) Color(0xFF56D0DE) else Color(0xFF2E3A52))
                                            ) {
                                                Text(
                                                    text = "Word",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (!isLine) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (!isLine) Color.White else Color(0xFF8FA7D8),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFF26344E),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Text(
                                text = "Choose a function to replace the button. Long-press the button anytime to change again:",
                                fontSize = 13.sp,
                                color = Color(0xFF8FA7D8),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            ReadingFunction.values().forEach { func ->
                                val isSelected = selectedFunction == func
                                Surface(
                                    onClick = {
                                        selectedFunction = func
                                        showFunctionPickerDialog = false
                                        viewModel.ttsWrapper.speakFeedback("Button set to ${func.title}")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.25f) else Color(0xFF1E283E),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF56D0DE) else Color(0xFF2E3A52)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (isSelected) Color(0xFF56D0DE).copy(alpha = 0.2f) else Color(0xFF161E30),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = func.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF56D0DE) else Color(0xFF8FA7D8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = func.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color(0xFF56D0DE) else Color(0xFFECEEF2)
                                            )
                                            Text(
                                                text = func.description,
                                                fontSize = 12.sp,
                                                color = Color(0xFF8FA7D8)
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF56D0DE),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFunctionPickerDialog = false }) {
                            Text("Close", color = Color(0xFF56D0DE), fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF141A28),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Voice Replace Popup reuse
            if (showReplacePopup) {
                val targetText = remember(contextMenuSelectedRange, textValue.text) {
                    val r = contextMenuSelectedRange
                    if (r != null && r.length > 0 && r.max <= textValue.text.length) {
                        textValue.text.substring(r.min, r.max)
                    } else null
                }
                ReplacePopup(
                    viewModel = viewModel,
                    initialTargetText = targetText,
                    onApplyReplace = { newText ->
                        val r = contextMenuSelectedRange
                        if (r != null && r.length > 0) {
                            viewModel.replaceRange(r, newText)
                            readingSelection = TextRange(r.min + newText.length)
                            contextMenuSelectedRange = null
                        } else {
                            viewModel.applyReplace(targetText ?: "", newText)
                        }
                    }
                )
            }

            // Text Size Dialog (Opened from 3-dot menu)
            if (showTextSizeDialog) {
                AlertDialog(
                    onDismissRequest = { showTextSizeDialog = false },
                    title = {
                        Text(
                            text = "Text Size",
                            color = Color(0xFFECEEF2),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val newSize = (settings.textSizeSp - 2f).coerceAtLeast(12f)
                                        viewModel.updateTextSize(newSize)
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF1E283E), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease Font Size",
                                        tint = Color(0xFFECEEF2)
                                    )
                                }

                                Text(
                                    text = "${settings.textSizeSp.toInt()} sp",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF56D0DE),
                                    modifier = Modifier.widthIn(min = 60.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val newSize = (settings.textSizeSp + 2f).coerceAtMost(48f)
                                        viewModel.updateTextSize(newSize)
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF1E283E), RoundedCornerShape(12.dp))
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${size.toInt()}",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFF8FA7D8)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTextSizeDialog = false }) {
                            Text("Done", color = Color(0xFF56D0DE), fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF141A28),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Highlight Color Picker Dialog (Opened from 3-dot menu)
            if (showColorPickerDialog) {
                AlertDialog(
                    onDismissRequest = { showColorPickerDialog = false },
                    title = {
                        Text(
                            text = "Highlight Color",
                            color = Color(0xFFECEEF2),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val colorOptions = listOf(
                                0xFF38BDF8L to "Sky Blue",
                                0xFF56D0DEL to "Cyan Teal",
                                0xFF2563EBL to "Royal Blue",
                                0xFF32D796L to "Mint Green",
                                0xFFFF6584L to "Coral Pink",
                                0xFFA855F7L to "Purple",
                                0xFFFF9800L to "Orange",
                                0xFFFFD600L to "Yellow"
                            )

                            colorOptions.chunked(4).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowColors.forEach { (colorHex, name) ->
                                        val isSelected = settings.highlightColorHex == colorHex
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.updateHighlightColor(colorHex)
                                                    viewModel.ttsWrapper.speakFeedback(name)
                                                    showColorPickerDialog = false
                                                }
                                                .padding(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(Color(colorHex), CircleShape)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) Color.White else Color(0x55FFFFFF),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = if ((Color(colorHex).red * 0.299f + Color(colorHex).green * 0.587f + Color(colorHex).blue * 0.114f) > 0.5f) Color.Black else Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = name,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color(0xFF56D0DE) else Color(0xFF8FA7D8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showColorPickerDialog = false }) {
                            Text("Close", color = Color(0xFF56D0DE), fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = Color(0xFF141A28),
                    shape = RoundedCornerShape(20.dp)
                )
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
