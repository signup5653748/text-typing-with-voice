package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ActionButton
import com.example.logic.CursorLogic
import com.example.ui.components.ActionButtonGrid
import com.example.ui.components.ArrowKeyCluster
import com.example.ui.components.VoiceMicButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacePopup(viewModel: EditorViewModel) {
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val partialResults by viewModel.speechWrapper.partialResults.collectAsState()
    val finalResult by viewModel.speechWrapper.finalResult.collectAsState()
    
    var previewText by remember { mutableStateOf(TextFieldValue("")) }
    
    LaunchedEffect(finalResult) {
        if (finalResult != null) {
            val textToInsert = finalResult!! + " "
            val start = previewText.selection.min
            val end = previewText.selection.max
            val newString = previewText.text.substring(0, start) + textToInsert + previewText.text.substring(end)
            previewText = TextFieldValue(newString, TextRange(start + textToInsert.length))
            viewModel.speechWrapper.clearResults()
        }
    }

    var kActive by remember { mutableStateOf(false) }
    var pActive by remember { mutableStateOf(false) }
    var selActive by remember { mutableStateOf(false) }
    var layoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    
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
            Text("Speak or type to create replacement text.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (previewText.text.isEmpty()) {
                    Text("Preview text...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                BasicTextField(
                    value = previewText,
                    onValueChange = { previewText = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    onTextLayout = { layoutResult = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                ActionButtonGrid(
                    buttonOrder = listOf(ActionButton.CUT, ActionButton.COPY, ActionButton.K, ActionButton.P, ActionButton.DELETE, ActionButton.PASTE, ActionButton.ENTER),
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
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                VoiceMicButton(
                    isListening = isListening,
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
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
                    onMoveUp = { previewText = CursorLogic.handleArrow(previewText, com.example.logic.ArrowDirection.UP, kActive, !pActive, selActive, layoutResult, null, null).first },
                    onMoveDown = { previewText = CursorLogic.handleArrow(previewText, com.example.logic.ArrowDirection.DOWN, kActive, !pActive, selActive, layoutResult, null, null).first },
                    onMoveLeft = { previewText = CursorLogic.handleArrow(previewText, com.example.logic.ArrowDirection.LEFT, kActive, !pActive, selActive, layoutResult, null, null).first },
                    onMoveRight = { previewText = CursorLogic.handleArrow(previewText, com.example.logic.ArrowDirection.RIGHT, kActive, !pActive, selActive, layoutResult, null, null).first },
                    onToggleSel = { selActive = !selActive },
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
