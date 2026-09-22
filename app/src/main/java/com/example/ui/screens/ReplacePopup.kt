package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.presentation.editor.EditorViewModel

/**
 * Brand-new clean, modern Material 3 Find & Replace Dialog.
 * Beautifully organized layout:
 * - Header with quick match badge and close button
 * - Target Find Card with match count badge and clear button
 * - Replacement Card with voice dictation, preview listen, and clear
 * - Action footer with numbered indicators and clear visual hierarchy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacePopup(
    viewModel: EditorViewModel,
    initialTargetText: String? = null,
    isInsertMode: Boolean = false,
    onApplyReplace: ((String) -> Unit)? = null
) {
    val isListening by viewModel.speechWrapper.isListening.collectAsState()
    val partialResults by viewModel.speechWrapper.partialResults.collectAsState()
    val finalResult by viewModel.speechWrapper.finalResult.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val textValue by viewModel.textValue.collectAsState()

    val context = LocalContext.current

    // Initialize find field with currently selected text or provided target
    val initialSelection = remember { initialTargetText ?: viewModel.getSelectedText() }
    var findText by remember { mutableStateOf(initialSelection) }
    var replaceWithText by remember { mutableStateOf("") }

    // When voice dictation finishes, update the replacement field
    LaunchedEffect(finalResult) {
        if (!finalResult.isNullOrBlank()) {
            val spoken = finalResult!!.trim()
            replaceWithText = if (replaceWithText.isEmpty()) {
                spoken
            } else {
                "$replaceWithText $spoken"
            }
            viewModel.ttsWrapper.speakFeedback(spoken)
            viewModel.speechWrapper.clearResults()
        }
    }

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (isListening) viewModel.speechWrapper.stopListening()
            else viewModel.speechWrapper.startListening(settings.voiceLanguage)
        }
    }

    fun toggleListening() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            if (isListening) viewModel.speechWrapper.stopListening()
            else viewModel.speechWrapper.startListening(settings.voiceLanguage)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Match count in the current document
    val matchCount = remember(findText, textValue.text) {
        if (findText.isBlank() || textValue.text.isEmpty()) 0
        else {
            var count = 0
            var idx = 0
            while (true) {
                val found = textValue.text.indexOf(findText, idx)
                if (found < 0) break
                count++
                idx = found + findText.length.coerceAtLeast(1)
            }
            count
        }
    }

    // Listening pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeReplacePopup() },
        containerColor = Color(0xFF0F1523),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF334155),
                height = 4.dp,
                width = 36.dp
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("replace_popup_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isInsertMode) "Insert Text" else "Find & Replace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = if (isListening) "Listening... speak text to insert" else if (isInsertMode) "Voice & text insert" else "Voice & text editing",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isListening) Color(0xFFFF6584) else Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.closeReplacePopup() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Listening status banner
            AnimatedVisibility(visible = isListening) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF311520),
                    border = BorderStroke(1.dp, Color(0xFFFF4B6E).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(pulseScale)
                                .background(Color(0xFFFF4B6E), CircleShape)
                        )
                        Text(
                            text = if (partialResults.isNotBlank()) partialResults else "Listening... speak replacement text now",
                            color = Color(0xFFFF8FA3),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Find Text Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF243048)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FIND TEXT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )

                        if (findText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (matchCount > 0) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFF334155).copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, if (matchCount > 0) Color(0xFF38BDF8) else Color(0xFF64748B))
                            ) {
                                Text(
                                    text = if (matchCount > 0) "$matchCount match${if (matchCount > 1) "es" else ""}" else "No matches",
                                    color = if (matchCount > 0) Color(0xFF7DD3FC) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = findText,
                        onValueChange = { findText = it },
                        placeholder = { Text("Enter text to find...", color = Color(0xFF64748B)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                        },
                        trailingIcon = {
                            if (findText.isNotEmpty()) {
                                IconButton(onClick = { findText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear find", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C101A),
                            unfocusedContainerColor = Color(0xFF0C101A),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Replace With Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161E30)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF243048)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "REPLACE WITH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = replaceWithText,
                        onValueChange = { replaceWithText = it },
                        placeholder = {
                            Text(
                                text = if (isListening) "Listening... speak now" else "Type or tap Speak below...",
                                color = if (isListening) Color(0xFF38BDF8) else Color(0xFF64748B)
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C101A),
                            unfocusedContainerColor = Color(0xFF0C101A),
                            focusedBorderColor = if (isListening) Color(0xFFFF4B6E) else Color(0xFF38BDF8),
                            unfocusedBorderColor = if (isListening) Color(0xFFFF4B6E).copy(alpha = 0.6f) else Color(0xFF334155),
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Toolbar: Speak, Listen, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Button (Speak)
                        Surface(
                            onClick = { toggleListening() },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isListening) Color(0xFFFF4B6E) else Color(0xFF0284C7),
                            border = BorderStroke(1.dp, if (isListening) Color(0xFFFF8DA1) else Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1.3f).height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isListening) "Stop" else "Speak",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // TTS Preview / Listen Button
                        Surface(
                            onClick = {
                                if (replaceWithText.isNotBlank()) {
                                    viewModel.ttsWrapper.speakFeedback(replaceWithText)
                                } else {
                                    viewModel.ttsWrapper.speakFeedback("No text to listen")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.weight(1.1f).height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Listen",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Listen",
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Clear Button
                        if (replaceWithText.isNotEmpty()) {
                            Surface(
                                onClick = { replaceWithText = "" },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Clear replacement",
                                        tint = Color(0xFFFF6584),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Buttons: Cancel, Replace All, Replace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel
                OutlinedButton(
                    onClick = { viewModel.closeReplacePopup() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                // Replace All (Only in Find & Replace Mode)
                if (!isInsertMode) {
                    Button(
                        onClick = {
                            viewModel.applyReplaceAll(findText, replaceWithText)
                        },
                        enabled = findText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFF38BDF8),
                            disabledContainerColor = Color(0xFF141926),
                            disabledContentColor = Color(0xFF475569)
                        ),
                        border = BorderStroke(1.dp, if (findText.isNotBlank()) Color(0xFF0284C7) else Color(0xFF1E293B)),
                        modifier = Modifier.weight(1.3f).height(48.dp)
                    ) {
                        Text(
                            text = if (matchCount > 1) "Replace All ($matchCount)" else "Replace All",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Replace / Insert
                Button(
                    onClick = {
                        if (onApplyReplace != null) {
                            onApplyReplace(replaceWithText)
                            viewModel.closeReplacePopup()
                        } else {
                            viewModel.applyReplace(findText, replaceWithText)
                        }
                    },
                    enabled = if (isInsertMode) replaceWithText.isNotBlank() else (findText.isNotBlank() || replaceWithText.isNotBlank()),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInsertMode) Color(0xFF10B981) else Color(0xFF0284C7),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF475569)
                    ),
                    modifier = Modifier.weight(if (isInsertMode) 1.5f else 1.2f).height(48.dp)
                ) {
                    Text(
                        text = if (isInsertMode) "Insert Text" else "Replace",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
