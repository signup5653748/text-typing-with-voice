package com.example.core.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
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
                            if (kActive) ModeIndicatorBadge(text = "K: CHAR", color = Color(0xFF56D0DE))
                            if (pActive) ModeIndicatorBadge(text = "P: PARA", color = Color(0xFF56D0DE))
                            if (selActive) ModeIndicatorBadge(text = "SEL ON", color = highlightColor)
                            if (kbLockActive) ModeIndicatorBadge(text = "KB LOCK", color = Color(0xFFFF6584))
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1420)),
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
                        Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color(0xFF8FA7D8))
                    }
                    IconButton(onClick = onPreviousMatch, enabled = searchMatches.isNotEmpty()) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Previous Result",
                            tint = if (searchMatches.isNotEmpty()) Color.White else Color(0xFF4A5568)
                        )
                    }
                    IconButton(onClick = onNextMatch, enabled = searchMatches.isNotEmpty()) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Next Result",
                            tint = if (searchMatches.isNotEmpty()) Color.White else Color(0xFF4A5568)
                        )
                    }
                }
            } else {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color(0xFF8FA7D8) else Color(0xFF4A5568)
                    )
                }
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF8FA7D8))
                }
                IconButton(onClick = { onMenuExpandedChange(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color(0xFF8FA7D8))
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
                            Icon(Icons.Default.MenuBook, contentDescription = "Reading Mode", tint = Color(0xFF56D0DE), modifier = Modifier.size(20.dp))
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
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) Color(0xFF56D0DE) else Color(0xFF4A5568), modifier = Modifier.size(20.dp))
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
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "New", tint = Color(0xFF56D0DE), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onNewFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open...", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Open", tint = Color(0xFFFFC700), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onOpenFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF32D796), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onSaveCurrentFile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save As...", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.SaveAs, contentDescription = "Save As", tint = Color(0xFF8FA7D8), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            onMenuExpandedChange(false)
                            onOpenSaveAsDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Jump to...", color = Color(0xFFECEEF2), fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "Jump to", tint = Color(0xFF56D0DE), modifier = Modifier.size(20.dp))
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
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF8FA7D8), modifier = Modifier.size(20.dp))
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
