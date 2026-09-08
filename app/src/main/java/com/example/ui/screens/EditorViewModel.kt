package com.example.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionButton
import com.example.data.ControlElement
import com.example.data.SettingsEntity
import com.example.data.SettingsRepository
import com.example.data.StarredFolder
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TTSWrapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    
    val speechWrapper = SpeechRecognitionWrapper(application)
    val ttsWrapper = TTSWrapper(application)

    val settings = settingsRepo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsEntity()
    )

    val starredFolders = settingsRepo.starredFolders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _isSaveAsMode = MutableStateFlow(false)
    val isSaveAsMode: StateFlow<Boolean> = _isSaveAsMode.asStateFlow()

    private val _textValue = MutableStateFlow(TextFieldValue(""))
    val textValue: StateFlow<TextFieldValue> = _textValue.asStateFlow()

    private val _showResumePopup = MutableStateFlow(false)
    val showResumePopup: StateFlow<Boolean> = _showResumePopup.asStateFlow()

    private val _savedDraft = MutableStateFlow<com.example.data.SessionDraft?>(null)
    val savedDraft: StateFlow<com.example.data.SessionDraft?> = _savedDraft.asStateFlow()

    private val _transientHighlightRange = MutableStateFlow<TextRange?>(null)
    val transientHighlightRange: StateFlow<TextRange?> = _transientHighlightRange.asStateFlow()

    // Default: K is false -> word by word; when true -> character by character
    private val _kActive = MutableStateFlow(false)
    val kActive: StateFlow<Boolean> = _kActive.asStateFlow()

    // Default: P is false -> line by line; when true -> paragraph by paragraph
    private val _pActive = MutableStateFlow(false)
    val pActive: StateFlow<Boolean> = _pActive.asStateFlow()

    private val _selActive = MutableStateFlow(false)
    val selActive: StateFlow<Boolean> = _selActive.asStateFlow()

    private val _kbLockActive = MutableStateFlow(false)
    val kbLockActive: StateFlow<Boolean> = _kbLockActive.asStateFlow()

    private val _currentFileUri = MutableStateFlow<android.net.Uri?>(null)
    val currentFileUri: StateFlow<android.net.Uri?> = _currentFileUri.asStateFlow()

    private val _fileName = MutableStateFlow("newfile.txt")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _showReplacePopup = MutableStateFlow(false)
    val showReplacePopup: StateFlow<Boolean> = _showReplacePopup.asStateFlow()

    private val _showLanguagePicker = MutableStateFlow(false)
    val showLanguagePicker: StateFlow<Boolean> = _showLanguagePicker.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<IntRange>>(emptyList())
    val searchMatches: StateFlow<List<IntRange>> = _searchMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    private var idealX: Float? = null
    private var selAnchor: Int? = null

    init {
        viewModelScope.launch {
            speechWrapper.finalResult.collect { result ->
                if (result != null && !_showReplacePopup.value) {
                    insertTextAtCursor(result + " ")
                    speechWrapper.clearResults()
                    ttsWrapper.speakFeedback(result)
                }
            }
        }
        
        viewModelScope.launch {
            ttsWrapper.currentRange.collect { range ->
                if (range != null) {
                    val current = _textValue.value
                    _textValue.value = current.copy(selection = TextRange(range.first, range.second))
                }
            }
        }

        viewModelScope.launch {
            settings.collect { s ->
                ttsWrapper.setLanguage(s.ttsLanguage)
                if (s.ttsEnginePackage.isNotBlank()) {
                    ttsWrapper.setEngine(s.ttsEnginePackage)
                }
            }
        }

        viewModelScope.launch {
            val draft = settingsRepo.lastSessionDraft.first()
            if (draft.text.isNotBlank()) {
                _savedDraft.value = draft
                _showResumePopup.value = true
            }
        }
    }

    private var persistJob: kotlinx.coroutines.Job? = null
    private fun persistDraft() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            settingsRepo.saveSessionDraft(
                com.example.data.SessionDraft(
                    text = _textValue.value.text,
                    fileName = _fileName.value,
                    uriString = _currentFileUri.value?.toString() ?: "",
                    selectionStart = _textValue.value.selection.start,
                    selectionEnd = _textValue.value.selection.end
                )
            )
        }
    }

    fun startNewDocument() {
        _textValue.value = TextFieldValue("")
        _fileName.value = "newfile.txt"
        _currentFileUri.value = null
        _showResumePopup.value = false
        resetCursorState()
        viewModelScope.launch {
            settingsRepo.clearSessionDraft()
        }
        ttsWrapper.speakFeedback("New blank document")
    }

    fun keepEditingDraft() {
        val draft = _savedDraft.value
        if (draft != null) {
            val clampedStart = draft.selectionStart.coerceIn(0, draft.text.length)
            val clampedEnd = draft.selectionEnd.coerceIn(0, draft.text.length)
            _textValue.value = TextFieldValue(draft.text, TextRange(clampedStart, clampedEnd))
            _fileName.value = if (draft.fileName.isNotBlank()) draft.fileName else "newfile.txt"
            if (draft.uriString.isNotBlank()) {
                _currentFileUri.value = Uri.parse(draft.uriString)
            } else {
                _currentFileUri.value = null
            }
            ttsWrapper.speakFeedback("Draft restored")
        }
        _showResumePopup.value = false
    }

    fun openLastSavedFile() {
        val draft = _savedDraft.value
        if (draft != null && draft.uriString.isNotBlank()) {
            loadFromUri(Uri.parse(draft.uriString))
        } else {
            keepEditingDraft()
        }
        _showResumePopup.value = false
    }

    fun dismissResumePopup() {
        _showResumePopup.value = false
    }

    fun onTextChanged(newValue: TextFieldValue) {
        if (_textValue.value == newValue) return
        _textValue.value = newValue
        if (_selActive.value) {
            _selActive.value = false
        }
        selAnchor = null
        idealX = null
        _transientHighlightRange.value = null
        persistDraft()
    }

    // Toggle K without resetting active selection
    fun toggleK() { 
        val newVal = !_kActive.value
        _kActive.value = newVal
        ttsWrapper.speakFeedback(if (newVal) "Character mode" else "Word mode")
    }

    // Toggle P without resetting active selection
    fun toggleP() { 
        val newVal = !_pActive.value
        _pActive.value = newVal
        ttsWrapper.speakFeedback(if (newVal) "Paragraph mode" else "Line mode")
    }
    
    fun toggleSel() { 
        val newSel = !_selActive.value
        _selActive.value = newSel
        val current = _textValue.value
        if (newSel) {
            // Lock anchor at current caret position
            selAnchor = current.selection.end
            _transientHighlightRange.value = null
            _textValue.value = current.copy(
                selection = TextRange(selAnchor!!, selAnchor!!),
                composition = null
            )
            ttsWrapper.speakFeedback("Selection mode on")
        } else {
            // Deselect: return cursor to normal single caret
            selAnchor = null
            val caret = current.selection.end
            _textValue.value = current.copy(
                selection = TextRange(caret, caret),
                composition = null
            )
            _transientHighlightRange.value = null
            ttsWrapper.speakFeedback("Selection mode off")
        }
    }
    
    fun toggleKbLock() { 
        val newVal = !_kbLockActive.value
        _kbLockActive.value = newVal
        ttsWrapper.speakFeedback(if (newVal) "Keyboard locked" else "Keyboard unlocked")
    }
    
    fun togglePlay() {
        if (ttsWrapper.isPlaying.value) {
            ttsWrapper.stop()
        } else {
            val current = _textValue.value
            val start = current.selection.min
            ttsWrapper.play(current.text, start)
        }
    }

    fun openLanguagePicker() { _showLanguagePicker.value = true }
    fun closeLanguagePicker() { _showLanguagePicker.value = false }
    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepo.updateVoiceLanguage(language)
            settingsRepo.updateTtsLanguage(language)
            ttsWrapper.setLanguage(language)
            closeLanguagePicker()
            val locName = try {
                java.util.Locale.forLanguageTag(language).displayName.ifBlank { language }
            } catch (_: Exception) {
                language
            }
            ttsWrapper.speakFeedback("Language set to $locName")
        }
    }

    fun updateTheme(themeName: String, bgHex: Long, textHex: Long, hlHex: Long) {
        viewModelScope.launch {
            settingsRepo.updateTheme(themeName, bgHex, textHex, hlHex)
        }
    }

    fun updateHighlightColor(colorHex: Long) {
        viewModelScope.launch {
            settingsRepo.updateHighlightColor(colorHex)
        }
    }

    fun updateBackgroundColor(colorHex: Long) {
        viewModelScope.launch {
            settingsRepo.updateBackgroundColor(colorHex)
        }
    }

    fun updateTextColor(colorHex: Long) {
        viewModelScope.launch {
            settingsRepo.updateTextColor(colorHex)
        }
    }

    fun updateTextSize(sizeSp: Float) {
        viewModelScope.launch {
            settingsRepo.updateTextSize(sizeSp)
        }
    }

    fun updateTtsLanguage(language: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsLanguage(language)
            ttsWrapper.setLanguage(language)
        }
    }

    fun updateTtsEngine(pkg: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsEnginePackage(pkg)
            ttsWrapper.setEngine(pkg)
        }
    }

    fun updateButtonSizeMultiplier(multiplier: Float) {
        viewModelScope.launch {
            settingsRepo.updateButtonSizeMultiplier(multiplier)
        }
    }

    fun updateButtonOrder(order: List<ActionButton>) {
        viewModelScope.launch {
            settingsRepo.updateButtonOrder(order)
        }
    }

    fun updateElementLayoutOrder(order: List<String>) {
        viewModelScope.launch {
            settingsRepo.updateElementLayoutOrder(order)
        }
    }

    fun toggleElementVisibility(elementName: String) {
        viewModelScope.launch {
            settingsRepo.toggleElementVisibility(elementName)
        }
    }

    fun updateArrowSize(size: Float) {
        viewModelScope.launch {
            settingsRepo.updateArrowSize(size)
        }
    }

    fun loadFromUri(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {}
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val text = inputStream.bufferedReader().readText()
                    _textValue.value = TextFieldValue(text)
                    _currentFileUri.value = uri
                    _fileName.value = getFileName(context.contentResolver, uri) ?: "unknown.txt"
                    resetCursorState()
                    ttsWrapper.speakFeedback("File opened")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveToUri(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {}
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(_textValue.value.text.toByteArray())
                    _currentFileUri.value = uri
                    _fileName.value = getFileName(context.contentResolver, uri) ?: "unknown.txt"
                    ttsWrapper.speakFeedback("File saved")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun openSaveDialog(isSaveAs: Boolean = false) {
        _isSaveAsMode.value = isSaveAs
        _showSaveDialog.value = true
    }

    fun closeSaveDialog() {
        _showSaveDialog.value = false
    }

    fun saveCurrentFile() {
        val uri = _currentFileUri.value
        if (uri != null) {
            saveToUri(uri)
        } else {
            openSaveDialog(isSaveAs = true)
        }
    }

    fun saveToFile(file: File) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (file.parentFile != null && !file.parentFile!!.exists()) {
                    file.parentFile!!.mkdirs()
                }
                file.writeText(_textValue.value.text)
                _currentFileUri.value = Uri.fromFile(file)
                _fileName.value = file.name
                closeSaveDialog()
                ttsWrapper.speakFeedback("Saved as ${file.name}")
            } catch (e: Exception) {
                e.printStackTrace()
                ttsWrapper.speakFeedback("Error saving file")
            }
        }
    }

    fun saveToStarredFolder(folder: StarredFolder, customFileName: String) {
        val rawName = if (customFileName.isNotBlank()) customFileName.trim() else _fileName.value
        val finalFileName = if (rawName.endsWith(".txt", ignoreCase = true)) rawName else "$rawName.txt"

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                if (folder.uriString.startsWith("content://")) {
                    val treeUri = Uri.parse(folder.uriString)
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: Exception) {}

                    val docUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri)
                    )
                    val newFileUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        docUri,
                        "text/plain",
                        finalFileName
                    )
                    if (newFileUri != null) {
                        context.contentResolver.openOutputStream(newFileUri)?.use { out ->
                            out.write(_textValue.value.text.toByteArray())
                        }
                        _currentFileUri.value = newFileUri
                        _fileName.value = finalFileName
                        closeSaveDialog()
                        ttsWrapper.speakFeedback("Saved to ${folder.name}")
                    } else {
                        ttsWrapper.speakFeedback("Could not save to folder")
                    }
                } else {
                    // Local directory
                    val dir = File(folder.uriString)
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, finalFileName)
                    file.writeText(_textValue.value.text)
                    _currentFileUri.value = Uri.fromFile(file)
                    _fileName.value = finalFileName
                    closeSaveDialog()
                    ttsWrapper.speakFeedback("Saved to ${folder.name}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ttsWrapper.speakFeedback("Error saving document")
            }
        }
    }

    fun addStarredFolderFromTreeUri(treeUri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                try {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                val docId = try { DocumentsContract.getTreeDocumentId(treeUri) } catch (e: Exception) { "Folder" }
                val rawFolderName = docId.substringAfterLast(':').substringAfterLast('/')
                val folderName = if (rawFolderName.isNotBlank()) rawFolderName else "Custom Folder"
                val pathDisplay = docId.replace(':', '/')

                val newFolder = StarredFolder(
                    id = UUID.randomUUID().toString(),
                    name = folderName,
                    pathDisplay = pathDisplay,
                    uriString = treeUri.toString(),
                    isDefault = false
                )
                settingsRepo.addStarredFolder(newFolder)
                ttsWrapper.speakFeedback("Starred folder $folderName added")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeStarredFolder(folderId: String) {
        viewModelScope.launch {
            settingsRepo.removeStarredFolder(folderId)
            ttsWrapper.speakFeedback("Folder unstarred")
        }
    }

    fun newFile() {
        _textValue.value = TextFieldValue("")
        _currentFileUri.value = null
        _fileName.value = "newfile.txt"
        resetCursorState()
        ttsWrapper.speakFeedback("New file")
    }

    private fun getFileName(contentResolver: android.content.ContentResolver, uri: android.net.Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore cursor query restrictions
            }
        }
        if (result.isNullOrBlank()) {
            result = uri.lastPathSegment?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return if (!result.isNullOrBlank()) result else "document.txt"
    }

    fun onMicClicked() {
        val currentSettings = settings.value
        if (speechWrapper.isListening.value) {
            speechWrapper.stopListening()
        } else {
            speechWrapper.startListening(currentSettings.voiceLanguage)
        }
    }

    fun openVoiceReplacePopup() {
        val currentSettings = settings.value
        _showReplacePopup.value = true
        speechWrapper.clearResults()
        speechWrapper.startListening(currentSettings.voiceLanguage)
    }

    fun closeReplacePopup() {
        _showReplacePopup.value = false
        speechWrapper.stopListening()
        speechWrapper.clearResults()
    }

    fun applyReplace(newText: String) {
        val current = _textValue.value
        val start = current.selection.min
        val end = current.selection.max
        val newString = current.text.substring(0, start) + newText + current.text.substring(end)
        _textValue.value = current.copy(text = newString, selection = TextRange(start + newText.length), composition = null)
        resetCursorState()
        closeReplacePopup()
        if (newText.isNotBlank()) {
            ttsWrapper.speakFeedback(newText)
        } else {
            ttsWrapper.speakFeedback("Replaced text")
        }
    }

    private fun insertTextAtCursor(textToInsert: String) {
        val current = _textValue.value
        val start = current.selection.min
        val end = current.selection.max
        val newString = current.text.substring(0, start) + textToInsert + current.text.substring(end)
        _textValue.value = current.copy(text = newString, selection = TextRange(start + textToInsert.length), composition = null)
        resetCursorState()
    }

    private fun resetCursorState() {
        _selActive.value = false
        selAnchor = null
        idealX = null
        _transientHighlightRange.value = null
        val current = _textValue.value
        if (current.selection.start != current.selection.end) {
            _textValue.value = current.copy(
                selection = TextRange(current.selection.end, current.selection.end),
                composition = null
            )
        }
    }

    private fun handleArrow(direction: ArrowDirection, layoutResult: TextLayoutResult? = null) {
        val prevCaret = _textValue.value.selection.end
        val res = CursorLogic.handleArrow(
            value = _textValue.value,
            direction = direction,
            isCharacterMode = _kActive.value, // K active: char by char; K off: word by word
            isParagraphMode = _pActive.value, // P active: paragraph by paragraph; P off: line by line
            isSelActive = _selActive.value,
            layoutResult = layoutResult,
            currentIdealX = idealX,
            currentSelAnchor = selAnchor
        )
        _textValue.value = res.value
        idealX = res.idealX
        selAnchor = res.selAnchor
        _transientHighlightRange.value = res.transientHighlightRange

        val newCaret = res.value.selection.end
        speakNavigationFeedback(direction, prevCaret, newCaret, layoutResult)
    }

    private fun speakNavigationFeedback(
        direction: ArrowDirection,
        prevCaret: Int,
        newCaret: Int,
        layoutResult: TextLayoutResult?
    ) {
        val text = _textValue.value.text
        if (text.isEmpty()) {
            ttsWrapper.speakFeedback("Empty document")
            return
        }

        if (newCaret == prevCaret) {
            if (newCaret <= 0) {
                ttsWrapper.speakFeedback("Beginning of document")
            } else if (newCaret >= text.length) {
                ttsWrapper.speakFeedback("End of document")
            }
            return
        }

        when (direction) {
            ArrowDirection.LEFT, ArrowDirection.RIGHT -> {
                if (_kActive.value) {
                    val charIdx = if (direction == ArrowDirection.LEFT) newCaret else (newCaret - 1).coerceAtLeast(0)
                    if (charIdx in text.indices) {
                        val ch = text[charIdx]
                        val spoken = when (ch) {
                            ' ' -> "Space"
                            '\n' -> "New line"
                            '\t' -> "Tab"
                            '.' -> "Dot"
                            ',' -> "Comma"
                            '!' -> "Exclamation mark"
                            '?' -> "Question mark"
                            ';' -> "Semicolon"
                            ':' -> "Colon"
                            '-' -> "Dash"
                            '_' -> "Underscore"
                            '/' -> "Slash"
                            '\\' -> "Backslash"
                            '(' -> "Open parenthesis"
                            ')' -> "Close parenthesis"
                            '\"' -> "Quote"
                            '\'' -> "Apostrophe"
                            else -> if (ch.isLetterOrDigit()) ch.toString() else "Symbol $ch"
                        }
                        ttsWrapper.speakFeedback(spoken)
                    }
                } else {
                    val wordIdx = if (direction == ArrowDirection.LEFT) newCaret else (newCaret - 1).coerceAtLeast(0)
                    val range = CursorLogic.getWordRangeAt(text, wordIdx)
                    val word = if (range.start < range.end && range.end <= text.length) {
                        text.substring(range.start, range.end).trim()
                    } else ""
                    if (word.isNotBlank()) {
                        ttsWrapper.speakFeedback(word)
                    } else {
                        ttsWrapper.speakFeedback("Space")
                    }
                }
            }
            ArrowDirection.UP, ArrowDirection.DOWN -> {
                if (_pActive.value) {
                    val pRange = CursorLogic.getParagraphRangeAt(text, newCaret)
                    val pText = if (pRange.start < pRange.end && pRange.end <= text.length) {
                        text.substring(pRange.start, pRange.end).trim()
                    } else ""
                    if (pText.isNotBlank()) {
                        ttsWrapper.speakFeedback(pText)
                    } else {
                        ttsWrapper.speakFeedback("Blank paragraph")
                    }
                } else {
                    val lineRange = if (layoutResult != null && layoutResult.lineCount > 0) {
                        val line = layoutResult.getLineForOffset(newCaret.coerceIn(0, text.length))
                        CursorLogic.getLineRange(text, layoutResult, line)
                    } else {
                        CursorLogic.getFallbackLineRange(text, newCaret)
                    }
                    val lineText = if (lineRange.start < lineRange.end && lineRange.end <= text.length) {
                        text.substring(lineRange.start, lineRange.end).trim()
                    } else ""
                    if (lineText.isNotBlank()) {
                        ttsWrapper.speakFeedback(lineText)
                    } else {
                        ttsWrapper.speakFeedback("Blank line")
                    }
                }
            }
        }
    }

    fun moveLeft() = handleArrow(ArrowDirection.LEFT)
    fun moveRight() = handleArrow(ArrowDirection.RIGHT)
    fun moveUp(layoutResult: TextLayoutResult?) = handleArrow(ArrowDirection.UP, layoutResult)
    fun moveDown(layoutResult: TextLayoutResult?) = handleArrow(ArrowDirection.DOWN, layoutResult)

    fun jumpStart() {
        val end = 0
        if (_selActive.value) {
            val anchor = selAnchor ?: _textValue.value.selection.end
            selAnchor = anchor
            _textValue.value = _textValue.value.copy(selection = TextRange(anchor, end), composition = null)
            _transientHighlightRange.value = null
        } else {
            _textValue.value = _textValue.value.copy(selection = TextRange(end, end), composition = null)
            _transientHighlightRange.value = CursorLogic.getWordRangeAt(_textValue.value.text, 0)
        }
        idealX = null
        ttsWrapper.speakFeedback("Top of document")
    }

    fun jumpEnd() {
        val end = _textValue.value.text.length
        if (_selActive.value) {
            val anchor = selAnchor ?: _textValue.value.selection.end
            selAnchor = anchor
            _textValue.value = _textValue.value.copy(selection = TextRange(anchor, end), composition = null)
            _transientHighlightRange.value = null
        } else {
            _textValue.value = _textValue.value.copy(selection = TextRange(end, end), composition = null)
            _transientHighlightRange.value = CursorLogic.getWordRangeAt(_textValue.value.text, if (end > 0) end - 1 else 0)
        }
        idealX = null
        ttsWrapper.speakFeedback("End of document")
    }

    fun onAction(action: ActionButton) {
        when (action) {
            ActionButton.K -> toggleK()
            ActionButton.P -> toggleP()
            else -> {
                val clipboardText = if (action == ActionButton.PASTE) pasteFromClipboard() else null
                _textValue.value = com.example.logic.TextActionLogic.handleAction(
                    action = action,
                    currentValue = _textValue.value,
                    clipboardText = clipboardText,
                    onCopy = { copyToClipboard(it) }
                )
                when (action) {
                    ActionButton.CUT -> ttsWrapper.speakFeedback("Cut")
                    ActionButton.COPY -> ttsWrapper.speakFeedback("Copied")
                    ActionButton.DELETE -> ttsWrapper.speakFeedback("Deleted")
                    ActionButton.PASTE -> ttsWrapper.speakFeedback("Pasted")
                    ActionButton.ENTER -> ttsWrapper.speakFeedback("Enter")
                    else -> {}
                }
                // Always return cursor to a normal single caret after Cut, Copy, Delete, Paste, Enter
                resetCursorState()
            }
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("VoiceType", text)
        clipboard.setPrimaryClip(clip)
    }

    fun pasteFromClipboard(): String {
        val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val item = clipboard.primaryClip?.getItemAt(0)
            return item?.text?.toString() ?: ""
        }
        return ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchMatches.value = emptyList()
            _currentMatchIndex.value = -1
            return
        }

        val text = _textValue.value.text
        val matches = mutableListOf<IntRange>()
        var index = 0
        while (index < text.length) {
            val found = text.indexOf(query, startIndex = index, ignoreCase = true)
            if (found >= 0) {
                matches.add(found until (found + query.length))
                index = found + query.length.coerceAtLeast(1)
            } else {
                break
            }
        }
        _searchMatches.value = matches
        if (matches.isNotEmpty()) {
            val currentPos = _textValue.value.selection.start
            var closestIdx = matches.indexOfFirst { it.first >= currentPos }
            if (closestIdx < 0) closestIdx = 0
            _currentMatchIndex.value = closestIdx
            highlightAndSpeakMatch(matches[closestIdx])
        } else {
            _currentMatchIndex.value = -1
        }
    }

    fun nextSearchMatch() {
        val matches = _searchMatches.value
        if (matches.isEmpty()) return
        val nextIdx = (_currentMatchIndex.value + 1) % matches.size
        _currentMatchIndex.value = nextIdx
        highlightAndSpeakMatch(matches[nextIdx])
    }

    fun previousSearchMatch() {
        val matches = _searchMatches.value
        if (matches.isEmpty()) return
        val prevIdx = if (_currentMatchIndex.value - 1 < 0) matches.size - 1 else _currentMatchIndex.value - 1
        _currentMatchIndex.value = prevIdx
        highlightAndSpeakMatch(matches[prevIdx])
    }

    private fun highlightAndSpeakMatch(range: IntRange) {
        val text = _textValue.value.text
        if (text.isEmpty()) return
        val start = range.first.coerceIn(0, text.length)
        val end = (range.last + 1).coerceIn(0, text.length)
        _textValue.value = _textValue.value.copy(
            selection = TextRange(start, end),
            composition = null
        )
        val matchedText = text.substring(start, end)
        if (matchedText.isNotBlank()) {
            ttsWrapper.play(matchedText, 0)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchMatches.value = emptyList()
        _currentMatchIndex.value = -1
        ttsWrapper.stop()
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsWrapper.shutdown()
    }
}
