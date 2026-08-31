

package com.example.ui.screens
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ActionButton
import com.example.ui.components.ActionButtonGrid
import com.example.ui.components.ArrowKeyCluster
import com.example.ui.components.VoiceMicButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val textValue by viewModel.textValue.collectAsState()
    val kActive by viewModel.kActive.collectAsState()
    val pActive by viewModel.pActive.collectAsState()
    val selActive by viewModel.selActive.collectAsState()
    val kbLockActive by viewModel.kbLockActive.collectAsState()
    val showReplacePopup by viewModel.showReplacePopup.collectAsState()
    val showLanguagePicker by viewModel.showLanguagePicker.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val fileName by viewModel.fileName.collectAsState()

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.loadFromUri(it) }
        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let { viewModel.saveToUri(it) }
        }
    )

    val buttonOrder = settings.buttonOrder.split(",").mapNotNull { 
        try { ActionButton.valueOf(it) } catch (e: Exception) { null }
    }
    val defaultOrder = listOf(ActionButton.CUT, ActionButton.COPY, ActionButton.K, ActionButton.P, ActionButton.DELETE, ActionButton.PASTE, ActionButton.ENTER)
    val actualOrder = if (buttonOrder.isEmpty()) defaultOrder else buttonOrder

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicClicked()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C0D10)),
                title = { Text(fileName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp) },
                actions = {
                    Text("Ready", fontSize = 11.sp, color = Color(0xFF9AA0AC), modifier = Modifier.padding(end = 10.dp))
                    com.example.ui.components.ModeToggleButton(
                        label = "KB",
                        isActive = kbLockActive,
                        onClick = viewModel::toggleKbLock,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("New") }, onClick = { viewModel.newFile(); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Open") }, onClick = { 
                                menuExpanded = false
                                openDocumentLauncher.launch(arrayOf("text/plain", "*/*")) 
                            })
                            DropdownMenuItem(text = { Text("Save") }, onClick = { 
                                menuExpanded = false
                                viewModel.saveCurrentFile {
                                    createDocumentLauncher.launch(fileName)
                                }
                            })
                            DropdownMenuItem(text = { Text("Save As") }, onClick = { 
                                menuExpanded = false
                                createDocumentLauncher.launch(fileName)
                            })
                            DropdownMenuItem(text = { Text("Share...") }, onClick = { menuExpanded = false })
                            DropdownMenuItem(text = { Text("Print") }, onClick = { menuExpanded = false })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { 
                                menuExpanded = false
                                onNavigateToSettings() 
                            })
                            DropdownMenuItem(text = { Text("Exit") }, onClick = { menuExpanded = false })
                        }
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF202228), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                if (textValue.text.isEmpty()) {
                    Text("Type something here", color = Color(0xFF5A5D66))
                }
                
                val customTextSelectionColors = TextSelectionColors(
                    handleColor = Color.Transparent,
                    backgroundColor = Color.Yellow.copy(alpha = 0.5f)
                )
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = viewModel::onTextChanged,
                        modifier = Modifier.fillMaxSize(),
                        readOnly = kbLockActive,
                        textStyle = TextStyle(
                            color = Color(0xFFECEEF2),
                            fontSize = 17.sp,
                            lineHeight = 25.5.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        onTextLayout = { layoutResult = it }
                    )
                }
            }
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
                                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
                        onMoveUp = { viewModel.moveUp(layoutResult) },
                        onMoveDown = { viewModel.moveDown(layoutResult) },
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
                                .clickable { viewModel.jumpStart() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Top", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color(0xFF9AA0AC), letterSpacing = 0.3.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF242730))
                                .clickable { viewModel.jumpEnd() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Bottom", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = Color(0xFF9AA0AC), letterSpacing = 0.3.sp)
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
