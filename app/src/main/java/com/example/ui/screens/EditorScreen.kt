package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
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
    onNavigateToSettings: () -> Unit,
    onNavigateToReadingMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val textValue by viewModel.textValue.collectAsState()
    val currentUri by viewModel.currentFileUri.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val kActive by viewModel.kActive.collectAsState()
    val pActive by viewModel.pActive.collectAsState()
    val selActive by viewModel.selActive.collectAsState()
    val kbLockActive by viewModel.kbLockActive.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val isPlaying by viewModel.ttsWrapper.isPlaying.collectAsState()
    val transientHighlightRange by viewModel.transientHighlightRange.collectAsState()
    val speechHighlightRange by viewModel.speechHighlightRange.collectAsState()

    val displayFileName = remember(fileName) {
        fileName.removeSuffix(".txt").removeSuffix(".TXT")
            .removeSuffix(".md").removeSuffix(".MD")
            .removeSuffix(".docx").removeSuffix(".DOCX")
    }

    var showMoreControlsSheetState by remember { mutableStateOf(false) }
    val showReplacePopup by viewModel.showReplacePopup.collectAsState()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }

    val showJumpToDialog by viewModel.showJumpToDialog.collectAsState()
    val jumpToHeadings by viewModel.jumpToHeadings.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }
    val buttonOrderList = remember(settings.buttonOrder) {
        val excluded = setOf("MORE", "REPLACE", "REP", "K", "P", "KB", "KB_LOCK", "TOP", "END", "JUMP", "JUMP_TO")
        val list = settings.buttonOrder.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() && it !in excluded }
        if (list.isNotEmpty()) list else listOf("CUT", "COPY", "DELETE", "PASTE", "SELECT_ALL", "ENTER")
    }

    val highlightColor = remember(settings.highlightColorHex) {
        Color(settings.highlightColorHex)
    }
    val editorBgColor = remember(settings.backgroundColorHex) {
        Color(settings.backgroundColorHex)
    }
    val textColor = remember(settings.textColorHex) {
        Color(settings.textColorHex)
    }

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
        if (uri != null) {
            viewModel.loadFromUri(uri, isFromExternalOrExplicitOpen = true)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.saveToUri(uri)
        }
    }

    Scaffold(
        containerColor = editorBgColor,
        topBar = {
            EditorTopBar(
                showSearchBar = showSearchBar,
                searchQuery = searchQuery,
                searchMatches = searchMatches,
                currentMatchIndex = currentMatchIndex,
                displayFileName = displayFileName,
                kActive = kActive,
                pActive = pActive,
                selActive = selActive,
                kbLockActive = kbLockActive,
                highlightColor = highlightColor,
                canUndo = canUndo,
                canRedo = canRedo,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onSearchQueryChange = viewModel::updateSearchQuery,
                onCloseSearch = {
                    showSearchBar = false
                    viewModel.clearSearch()
                },
                onPreviousMatch = { viewModel.previousSearchMatch() },
                onNextMatch = { viewModel.nextSearchMatch() },
                onUndo = { viewModel.undo() },
                onOpenSearch = {
                    showSearchBar = true
                    if (searchQuery.isNotEmpty()) {
                        viewModel.updateSearchQuery(searchQuery)
                    }
                },
                onNavigateToReadingMode = onNavigateToReadingMode,
                onRedo = { viewModel.redo() },
                onNewFile = { viewModel.newFile() },
                onOpenFile = {
                    openDocumentLauncher.launch(
                        arrayOf(
                            "text/*",
                            "text/plain",
                            "text/markdown",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                onSaveCurrentFile = { viewModel.saveCurrentFile() },
                onOpenSaveAsDialog = { viewModel.openSaveDialog(isSaveAs = true) },
                onOpenJumpTo = { viewModel.openJumpTo() },
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Text Canvas with Floating READ and MIC buttons
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EditorTextArea(
                    viewModel = viewModel,
                    kbLockActive = kbLockActive,
                    transientHighlightRange = transientHighlightRange,
                    speechHighlightRange = speechHighlightRange,
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
                    // READ Button (Single click only = instantly reads text aloud. No long-press action)
                    FloatingReadButton(
                        isPlaying = isPlaying,
                        onClick = viewModel::togglePlay
                    )

                    // MIC Button:
                    // When alwaysInsertMicDirectly = true (default): always inserts dictation directly at caret
                    // When alwaysInsertMicDirectly = false:
                    //   - If selection active -> Voice Replace Mode
                    //   - If no selection -> Normal Voice Typing
                    // Long-press = Opens Language Picker (controls speech-to-text and read-aloud language)
                    FloatingMicButton(
                        isListening = isListening,
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                if (settings.alwaysInsertMicDirectly) {
                                    viewModel.onMicClicked()
                                } else {
                                    val hasSelection = selActive || (textValue.selection.start != textValue.selection.end && textValue.text.isNotEmpty())
                                    if (hasSelection) {
                                        viewModel.openVoiceReplacePopup()
                                    } else {
                                        viewModel.onMicClicked()
                                    }
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onLongClick = {
                            viewModel.openLanguagePicker()
                        }
                    )
                }
            }

            // Bottom Control Area (Quick Navigation Row + Keypad Matrix & Arrow Cluster)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0E18))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Compact Mode Toggles & Quick Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickToggleButton(
                        label = "K",
                        subLabel = if (kActive) "CHAR" else "WORD",
                        isActive = kActive,
                        activeColor = Color(0xFF56D0DE),
                        onClick = viewModel::toggleK,
                        modifier = Modifier.weight(1f)
                    )
                    QuickToggleButton(
                        label = "P",
                        subLabel = if (pActive) "PARA" else "LINE",
                        isActive = pActive,
                        activeColor = Color(0xFF56D0DE),
                        onClick = viewModel::toggleP,
                        modifier = Modifier.weight(1f)
                    )
                    QuickToggleButton(
                        label = "KB",
                        subLabel = if (kbLockActive) "LOCK" else "UNLK",
                        isActive = kbLockActive,
                        activeColor = Color(0xFFFF6584),
                        onClick = viewModel::toggleKbLock,
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolButton(
                        icon = Icons.Default.VerticalAlignTop,
                        label = "TOP",
                        onClick = viewModel::jumpStart,
                        modifier = Modifier.weight(0.9f)
                    )
                    QuickToolButton(
                        icon = Icons.Default.VerticalAlignBottom,
                        label = "END",
                        onClick = viewModel::jumpEnd,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // 2 Equal-Width Panels: Action Grid (Left 1f) and Arrow Cluster (Right 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Action Grid
                    ActionButtonGrid(
                        onActionClick = viewModel::onAction,
                        buttonOrder = buttonOrderList,
                        sizeMultiplier = settings.buttonSizeMultiplier,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    // Right Side: 3x3 D-Pad Directional Cluster
                    ArrowKeyCluster(
                        selActive = selActive,
                        scale = settings.arrowSize,
                        onMoveUp = { viewModel.moveUp(layoutResult.value) },
                        onMoveDown = { viewModel.moveDown(layoutResult.value) },
                        onMoveLeft = viewModel::moveLeft,
                        onMoveRight = viewModel::moveRight,
                        onToggleSel = viewModel::toggleSel,
                        activeHighlightColor = highlightColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    if (showMoreControlsSheetState) {
        MoreControlsSheet(
            viewModel = viewModel,
            onNavigateToSettings = onNavigateToSettings,
            onDismiss = { showMoreControlsSheetState = false }
        )
    }

    if (showReplacePopup) {
        ReplacePopup(viewModel)
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(viewModel)
    }

    if (showSaveDialog) {
        SaveFileDialog(
            viewModel = viewModel,
            onBrowseSystemFolders = { fileNameToSave ->
                createDocumentLauncher.launch(fileNameToSave)
            }
        )
    }

    if (showJumpToDialog) {
        JumpToDialog(
            headings = jumpToHeadings,
            onSelectHeading = { viewModel.jumpToHeading(it) },
            onDismiss = { viewModel.closeJumpTo() }
        )
    }

    SessionResumeDialog(viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    showSearchBar: Boolean,
    searchQuery: String,
    searchMatches: List<IntRange>,
    currentMatchIndex: Int,
    displayFileName: String,
    kActive: Boolean,
    pActive: Boolean,
    selActive: Boolean,
    kbLockActive: Boolean,
    highlightColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onUndo: () -> Unit,
    onOpenSearch: () -> Unit,
    onNavigateToReadingMode: () -> Unit,
    onRedo: () -> Unit,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveCurrentFile: () -> Unit,
    onOpenSaveAsDialog: () -> Unit,
    onOpenJumpTo: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    TopAppBar(
        title = {
            if (showSearchBar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search text...", color = Color(0xFF6B7FA8), fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column {
                    Text(
                        text = displayFileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFECEEF2)
                    )
                    if (kActive || pActive || selActive || kbLockActive) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (kActive) {
                                ModeIndicatorBadge(text = "K: CHAR", color = Color(0xFF56D0DE))
                            }
                            if (pActive) {
                                ModeIndicatorBadge(text = "P: PARA", color = Color(0xFF56D0DE))
                            }
                            if (selActive) {
                                ModeIndicatorBadge(text = "SEL ON", color = highlightColor)
                            }
                            if (kbLockActive) {
                                ModeIndicatorBadge(text = "KB LOCK", color = Color(0xFFFF6584))
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0F1420)
        ),
        actions = {
            if (showSearchBar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchMatches.isNotEmpty()) {
                        Text(
                            text = "${currentMatchIndex + 1}/${searchMatches.size}",
                            color = Color(0xFF56D0DE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onCloseSearch) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = Color(0xFF8FA7D8)
                        )
                    }

                    // Previous Match
                    IconButton(
                        onClick = onPreviousMatch,
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
                        onClick = onNextMatch,
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
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color(0xFF8FA7D8) else Color(0xFF4A5568)
                    )
                }

                IconButton(onClick = onOpenSearch) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF8FA7D8)
                    )
                }

                IconButton(onClick = { onMenuExpandedChange(true) }) {
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
                    onDismissRequest = { onMenuExpandedChange(false) },
                    modifier = Modifier
                        .width(230.dp)
                        .background(Color(0xFF161E30))
                        .border(1.dp, Color(0xFF26344E), RoundedCornerShape(14.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Reading Mode", color = Color(0xFFECEEF2), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Reading Mode",
                                tint = Color(0xFF56D0DE),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onNavigateToReadingMode()
                        }
                    )
                    HorizontalDivider(color = Color(0xFF222B3F), modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Redo", color = if (canRedo) Color(0xFFECEEF2) else Color(0xFF6B7FA8), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) Color(0xFF56D0DE) else Color(0xFF4A5568),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        enabled = canRedo,
                        onClick = {
                            onMenuExpandedChange(false)
                            onRedo()
                        }
                    )
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
                            onMenuExpandedChange(false)
                            onNewFile()
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
                            onMenuExpandedChange(false)
                            onOpenFile()
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
                            onMenuExpandedChange(false)
                            onSaveCurrentFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save As...", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SaveAs,
                                contentDescription = "Save As",
                                tint = Color(0xFF8FA7D8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onOpenSaveAsDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Jump to...", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Jump to",
                                tint = Color(0xFF56D0DE),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onOpenJumpTo()
                        }
                    )
                    HorizontalDivider(color = Color(0xFF222B3F), modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("Settings", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF8FA7D8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onNavigateToSettings()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun QuickToggleButton(
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 11.sp
            )
            Text(
                text = subLabel,
                color = subTextColor,
                fontWeight = FontWeight.Medium,
                fontSize = 8.sp,
                lineHeight = 8.sp
            )
        }
    }
}

@Composable
private fun QuickToolButton(
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
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF8FA7D8),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = Color(0xFFECEFF8),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
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
    onLongClick: (() -> Unit)? = null,
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
fun ModeIndicatorBadge(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

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
    viewModel: EditorViewModel,
    kbLockActive: Boolean,
    transientHighlightRange: TextRange?,
    speechHighlightRange: TextRange?,
    onTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val textValue by viewModel.textValue.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val highlightColor = remember(settings.highlightColorHex) { Color(settings.highlightColorHex) }
    val textColor = remember(settings.textColorHex) { Color(settings.textColorHex) }
    val textSize = settings.textSizeSp.sp
    val lineHeight = (settings.textSizeSp * 1.4f).sp

    val focusRequester = remember { FocusRequester() }
    var localLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Blinking cursor animation - declarative transition scoped only to cursor overlay
    val isCursorVisible = !kbLockActive && textValue.selection.collapsed && transientHighlightRange == null && speechHighlightRange == null

    Box(
        modifier = modifier.then(
            if (kbLockActive) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        keyboardController?.hide()
                        localLayoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(tapOffset)
                            viewModel.setCaretFromTap(offset)
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
                visualTransformation = remember(textValue.selection, transientHighlightRange, speechHighlightRange, highlightColor, settings.hideHeadingSymbols) {
                    val isLightHighlight = (highlightColor.red * 0.299f + highlightColor.green * 0.587f + highlightColor.blue * 0.114f) > 0.45f
                    SelectionHighlightTransformation(
                        selection = textValue.selection,
                        transientHighlight = transientHighlightRange,
                        speechHighlight = speechHighlightRange,
                        highlightColor = highlightColor.copy(alpha = 0.7f),
                        speechHighlightColor = Color(0xFF00E5FF),
                        highlightedTextColor = if (isLightHighlight) Color(0xFF0D111A) else Color.White,
                        hideHeadingSymbols = settings.hideHeadingSymbols
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
            BlinkingCursorOverlay(
                layout = localLayoutResult,
                caretOffset = textValue.selection.end,
                highlightColor = highlightColor,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun BlinkingCursorOverlay(
    layout: TextLayoutResult?,
    caretOffset: Int,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
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
        if (layout != null) {
            try {
                val maxLayoutOffset = layout.layoutInput.text.length
                val caret = caretOffset.coerceIn(0, maxLayoutOffset)
                val rect = layout.getCursorRect(caret)
                drawRoundRect(
                    color = highlightColor.copy(alpha = cursorAlpha),
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
