package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.editor.dialogs.CurrentDirectory
import com.example.core.editor.dialogs.DirectoryLoader
import com.example.core.editor.dialogs.FileEntry
import com.example.core.editor.dialogs.FileOrFolderRow
import com.example.core.editor.dialogs.NewFolderDialog
import com.example.core.editor.dialogs.OverwriteConfirmDialog
import com.example.core.editor.dialogs.StarredFolderCard
import com.example.data.StarredFolder
import com.example.presentation.editor.EditorViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveFileDialog(
    viewModel: EditorViewModel,
    onBrowseSystemFolders: (String) -> Unit
) {
    val context = LocalContext.current
    val starredFolders by viewModel.starredFolders.collectAsState()
    val isSaveAsMode by viewModel.isSaveAsMode.collectAsState()
    val currentFileName by viewModel.fileName.collectAsState()

    var selectedFormat by remember(currentFileName) {
        mutableStateOf(com.example.logic.SupportedFileType.fromFileName(currentFileName))
    }

    var inputFileName by remember(currentFileName) {
        val base = currentFileName
            .removeSuffix(".txt").removeSuffix(".TXT")
            .removeSuffix(".md").removeSuffix(".MD")
            .removeSuffix(".docx").removeSuffix(".DOCX")
        mutableStateOf(base)
    }

    // Active folder navigation state: null = root starred folders view, or current directory path / SAF uri
    var currentFolderState by remember { mutableStateOf<CurrentDirectory?>(null) }
    var folderEntries by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var overwriteTargetFile by remember { mutableStateOf<FileEntry?>(null) }

    // Load directory entries whenever currentFolderState changes
    LaunchedEffect(currentFolderState) {
        val folder = currentFolderState
        if (folder != null) {
            folderEntries = DirectoryLoader.loadEntriesForDirectory(context, folder)
        } else {
            folderEntries = emptyList()
        }
    }

    // Back handler for layer-by-layer navigation
    BackHandler(enabled = true) {
        if (overwriteTargetFile != null) {
            overwriteTargetFile = null
        } else if (showNewFolderDialog) {
            showNewFolderDialog = false
        } else if (currentFolderState != null) {
            val parent = currentFolderState?.parent
            currentFolderState = parent
        } else {
            viewModel.closeSaveDialog()
        }
    }

    // Launcher to star a new SAF folder
    val pickFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.addStarredFolderFromTreeUri(treeUri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeSaveDialog() },
        containerColor = Color(0xFF141A29),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding()
        ) {
            // Top Bar: Navigation / Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentFolderState != null) {
                        IconButton(
                            onClick = {
                                currentFolderState = currentFolderState?.parent
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF56D0DE)
                            )
                        }
                    } else {
                        Icon(
                            if (isSaveAsMode) Icons.Default.DriveFolderUpload else Icons.Default.Save,
                            contentDescription = null,
                            tint = Color(0xFF56D0DE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = currentFolderState?.displayName ?: if (isSaveAsMode) "Save As..." else "Save Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentFolderState != null) {
                            Text(
                                text = currentFolderState?.displayPath ?: "",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.closeSaveDialog() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8FA7D8))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File Name Input Field
            Text(
                "File Name",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = inputFileName,
                onValueChange = { inputFileName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("document_name", color = Color(0xFF64748B)) },
                trailingIcon = {
                    Surface(
                        color = Color(0xFF202A3C),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            ".${selectedFormat.extension}",
                            color = Color(0xFF56D0DE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFECEFF8),
                    unfocusedTextColor = Color(0xFFECEFF8),
                    focusedContainerColor = Color(0xFF0F1420),
                    unfocusedContainerColor = Color(0xFF0F1420),
                    focusedBorderColor = Color(0xFF56D0DE),
                    unfocusedBorderColor = Color(0xFF2A364F)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Format Selection Chips (.txt, .md, .docx)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Format:",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                com.example.logic.SupportedFileType.values().forEach { type ->
                    val isSelected = selectedFormat == type
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF56D0DE) else Color(0xFF26334D),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedFormat = type },
                        color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF131A29)
                    ) {
                        Text(
                            text = ".${type.extension}",
                            color = if (isSelected) Color(0xFF56D0DE) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content: Either Folder Browser or Starred Folders Root
            val activeFolder = currentFolderState
            if (activeFolder == null) {
                // ROOT VIEW: Starred Folders First
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Starred Folders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8)
                        )
                    }

                    TextButton(
                        onClick = { pickFolderLauncher.launch(null) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF56D0DE))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Star Folder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (starredFolders.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                color = Color(0xFF0F1420),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243048))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.FolderSpecial,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "No starred folders yet",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(starredFolders, key = { it.id }) { folder ->
                            StarredFolderCard(
                                folder = folder,
                                onOpenFolder = {
                                    currentFolderState = CurrentDirectory(
                                        displayName = folder.name,
                                        displayPath = folder.pathDisplay,
                                        uriOrPath = folder.uriString,
                                        isTreeUri = folder.uriString.startsWith("content://"),
                                        parent = null
                                    )
                                },
                                onFastSave = {
                                    val base = if (inputFileName.isBlank()) "document" else inputFileName
                                    val nameToUse = if (base.contains('.')) base else "$base.${selectedFormat.extension}"
                                    viewModel.saveToStarredFolder(folder, nameToUse)
                                },
                                onRemove = if (!folder.isDefault) {
                                    { viewModel.removeStarredFolder(folder.id) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = Color(0xFF243048))

                Spacer(modifier = Modifier.height(10.dp))

                // Browse all folders button
                OutlinedButton(
                    onClick = {
                        viewModel.closeSaveDialog()
                        val base = if (inputFileName.isBlank()) "document" else inputFileName
                        val nameToUse = if (base.contains('.')) base else "$base.${selectedFormat.extension}"
                        onBrowseSystemFolders(nameToUse)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B3A54)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF101726),
                        contentColor = Color(0xFFECEFF8)
                    )
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFFFFC700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Browse all folders...",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            } else {
                // NAVIGATED FOLDER VIEW: Browse subfolders and files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val base = if (inputFileName.isBlank()) "document" else inputFileName
                            val nameToUse = if (base.contains('.')) base else "$base.${selectedFormat.extension}"
                            if (!activeFolder.isTreeUri) {
                                val targetFile = File(activeFolder.uriOrPath, nameToUse)
                                viewModel.saveToFile(targetFile)
                            } else {
                                val starred = StarredFolder(
                                    id = "temp",
                                    name = activeFolder.displayName,
                                    pathDisplay = activeFolder.displayPath,
                                    uriString = activeFolder.uriOrPath
                                )
                                viewModel.saveToStarredFolder(starred, nameToUse)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save In This Folder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (!activeFolder.isTreeUri) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                newFolderNameInput = ""
                                showNewFolderDialog = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                Icons.Default.CreateNewFolder,
                                contentDescription = "New Folder",
                                tint = Color(0xFF56D0DE),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (folderEntries.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp)),
                                color = Color(0xFF0F1420),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243048))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "This folder is empty",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Tap 'Save In This Folder' above to save here",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(folderEntries, key = { it.path }) { entry ->
                            FileOrFolderRow(
                                entry = entry,
                                onFolderClick = {
                                    currentFolderState = CurrentDirectory(
                                        displayName = entry.name,
                                        displayPath = "${activeFolder.displayPath}/${entry.name}",
                                        uriOrPath = entry.path,
                                        isTreeUri = false,
                                        parent = activeFolder
                                    )
                                },
                                onFileClick = {
                                    val cleaned = entry.name
                                        .removeSuffix(".txt").removeSuffix(".TXT")
                                        .removeSuffix(".md").removeSuffix(".MD")
                                        .removeSuffix(".docx").removeSuffix(".DOCX")
                                    inputFileName = cleaned
                                    selectedFormat = com.example.logic.SupportedFileType.fromFileName(entry.name)
                                    overwriteTargetFile = entry
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Dialog for creating a new subfolder
    if (showNewFolderDialog && currentFolderState != null) {
        NewFolderDialog(
            folderNameInput = newFolderNameInput,
            onFolderNameInputChange = { newFolderNameInput = it },
            onConfirm = {
                val name = newFolderNameInput.trim()
                if (name.isNotBlank() && currentFolderState != null) {
                    val active = currentFolderState!!
                    if (!active.isTreeUri) {
                        val newDir = File(active.uriOrPath, name)
                        newDir.mkdirs()
                        folderEntries = DirectoryLoader.loadEntriesForDirectory(context, active)
                    }
                }
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    // Dialog for confirming overwrite of an existing file
    if (overwriteTargetFile != null) {
        val target = overwriteTargetFile!!
        OverwriteConfirmDialog(
            fileName = target.name,
            onConfirm = {
                if (target.fileRef != null) {
                    viewModel.saveToFile(target.fileRef)
                } else {
                    val active = currentFolderState
                    if (active != null) {
                        val starred = StarredFolder(
                            id = "temp",
                            name = active.displayName,
                            pathDisplay = active.displayPath,
                            uriString = active.uriOrPath
                        )
                        viewModel.saveToStarredFolder(starred, target.name)
                    }
                }
                overwriteTargetFile = null
            },
            onDismiss = { overwriteTargetFile = null }
        )
    }
}
