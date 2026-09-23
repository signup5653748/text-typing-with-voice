package com.example.presentation.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.editor.ui.EditorTextArea
import com.example.core.editor.ui.EditorTopBar
import com.example.core.editor.ui.FloatingMicButton
import com.example.core.editor.ui.FloatingReadButton
import com.example.core.editor.ui.QuickToggleButton
import com.example.core.editor.ui.QuickToolButton
import com.example.ui.components.ActionButtonGrid
import com.example.ui.components.ArrowKeyCluster
import com.example.ui.screens.JumpToDialog
import com.example.ui.screens.LanguagePickerSheet
import com.example.ui.screens.MoreControlsSheet
import com.example.ui.screens.ReplacePopup
import com.example.ui.screens.SaveFileDialog
import com.example.ui.screens.SessionResumeDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToReadingMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val textValue by viewModel.textValue.collectAsState()
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

    val highlightColor = remember(settings.highlightColorHex) { Color(settings.highlightColorHex) }
    val editorBgColor = remember(settings.backgroundColorHex) { Color(settings.backgroundColorHex) }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.onMicClicked()
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.loadFromUri(uri, isFromExternalOrExplicitOpen = true)
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) viewModel.saveToUri(uri)
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
                    if (searchQuery.isNotEmpty()) viewModel.updateSearchQuery(searchQuery)
                },
                onNavigateToReadingMode = onNavigateToReadingMode,
                onRedo = { viewModel.redo() },
                onNewFile = { viewModel.newFile() },
                onOpenFile = {
                    openDocumentLauncher.launch(
                        arrayOf(
                            "text/*", "text/plain", "text/markdown",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword", "application/octet-stream", "*/*"
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EditorTextArea(
                    textValue = textValue,
                    settings = settings,
                    kbLockActive = kbLockActive,
                    transientHighlightRange = transientHighlightRange,
                    speechHighlightRange = speechHighlightRange,
                    onTextChanged = viewModel::onTextChanged,
                    onCaretTap = viewModel::setCaretFromTap,
                    onTextLayout = { layoutResult.value = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingReadButton(isPlaying = isPlaying, onClick = viewModel::togglePlay)
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
                                    if (hasSelection) viewModel.openVoiceReplacePopup() else viewModel.onMicClicked()
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onLongClick = { viewModel.openLanguagePicker() }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0E18))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButtonGrid(
                        onActionClick = viewModel::onAction,
                        buttonOrder = buttonOrderList,
                        sizeMultiplier = settings.buttonSizeMultiplier,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

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
