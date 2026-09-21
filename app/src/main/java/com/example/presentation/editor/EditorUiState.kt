package com.example.presentation.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.SessionDraft
import com.example.data.SettingsEntity
import com.example.data.StarredFolder
import com.example.logic.HeadingItem

data class EditorUiState(
    val textValue: TextFieldValue = TextFieldValue(""),
    val currentFileUri: Uri? = null,
    val fileName: String = "newfile.txt",
    val kActive: Boolean = false,
    val pActive: Boolean = false,
    val selActive: Boolean = false,
    val kbLockActive: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showReplacePopup: Boolean = false,
    val showLanguagePicker: Boolean = false,
    val showSaveDialog: Boolean = false,
    val isSaveAsMode: Boolean = false,
    val showResumePopup: Boolean = false,
    val transientHighlightRange: TextRange? = null,
    val speechHighlightRange: TextRange? = null,
    val searchQuery: String = "",
    val searchMatches: List<IntRange> = emptyList(),
    val currentMatchIndex: Int = -1,
    val showJumpToDialog: Boolean = false,
    val jumpToHeadings: List<HeadingItem> = emptyList(),
    val savedDraft: SessionDraft? = null,
    val settings: SettingsEntity = SettingsEntity(),
    val starredFolders: List<StarredFolder> = emptyList()
)
