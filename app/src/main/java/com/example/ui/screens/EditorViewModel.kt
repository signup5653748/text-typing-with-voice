package com.example.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionButton
import com.example.data.SessionDraft
import com.example.data.SettingsEntity
import com.example.data.SettingsRepository
import com.example.data.StarredFolder
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.logic.HeadingItem
import com.example.logic.HeadingLogic
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TTSWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class EditorViewModel(
    application: Application,
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, SettingsRepository(application))

    val ttsWrapper = TTSWrapper(application)
    val speechWrapper = SpeechRecognitionWrapper(application)

    private val _textValue = MutableStateFlow(TextFieldValue(""))
    val textValue: StateFlow<TextFieldValue> = _textValue.asStateFlow()

    private val _currentFileUri = MutableStateFlow<Uri?>(null)
    val currentFileUri: StateFlow<Uri?> = _currentFileUri.asStateFlow()

    private val _fileName = MutableStateFlow("newfile.txt")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _kActive = MutableStateFlow(false)
    val kActive: StateFlow<Boolean> = _kActive.asStateFlow()

    private val _pActive = MutableStateFlow(false)
    val pActive: StateFlow<Boolean> = _pActive.asStateFlow()

    // Undo & Redo history stacks
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    private var isPerformingUndoRedo = false

    private val _selActive = MutableStateFlow(false)
    val selActive: StateFlow<Boolean> = _selActive.asStateFlow()

    private val _kbLockActive = MutableStateFlow(false)
    val kbLockActive: StateFlow<Boolean> = _kbLockActive.asStateFlow()

    private val _showReplacePopup = MutableStateFlow(false)
    val showReplacePopup: StateFlow<Boolean> = _showReplacePopup.asStateFlow()

    private val _showLanguagePicker = MutableStateFlow(false)
    val showLanguagePicker: StateFlow<Boolean> = _showLanguagePicker.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _isSaveAsMode = MutableStateFlow(false)
    val isSaveAsMode: StateFlow<Boolean> = _isSaveAsMode.asStateFlow()

    private val _showResumePopup = MutableStateFlow(false)
    val showResumePopup: StateFlow<Boolean> = _showResumePopup.asStateFlow()

    val savedDraft: StateFlow<SessionDraft?> = settingsRepo.sessionDraftFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Transient word/line highlight for TTS navigation readouts
    private val _transientHighlightRange = MutableStateFlow<TextRange?>(null)
    val transientHighlightRange: StateFlow<TextRange?> = _transientHighlightRange.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<IntRange>>(emptyList())
    val searchMatches: StateFlow<List<IntRange>> = _searchMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    // Jump to state & in-memory session cache
    private var cachedHeadings: List<HeadingItem>? = null
    private val _showJumpToDialog = MutableStateFlow(false)
    val showJumpToDialog: StateFlow<Boolean> = _showJumpToDialog.asStateFlow()

    private val _jumpToHeadings = MutableStateFlow<List<HeadingItem>>(emptyList())
    val jumpToHeadings: StateFlow<List<HeadingItem>> = _jumpToHeadings.asStateFlow()

    private var isExplicitDocumentSession = false

    val settings: StateFlow<SettingsEntity> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsEntity()
    )

    val starredFolders: StateFlow<List<StarredFolder>> = settingsRepo.starredFoldersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // State tracking for smooth navigation & selection
    private var selAnchor: Int? = null
    private var idealX: Float? = null

    init {
        // Collect recognized speech
        viewModelScope.launch {
            speechWrapper.finalResult.collect { result ->
                if (!result.isNullOrBlank()) {
                    if (_showReplacePopup.value) {
                        applyReplace(result)
                    } else {
                        insertTextAtCursor(result)
                        ttsWrapper.speakFeedback(result)
                    }
                }
            }
        }

        // Apply initial settings to TTS
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                ttsWrapper.setSpeechRate(s.ttsSpeed)
                ttsWrapper.setPitch(s.ttsPitch)
                if (s.ttsLanguage.isNotEmpty()) {
                    ttsWrapper.setLanguage(s.ttsLanguage)
                }
                if (s.ttsEnginePackage.isNotEmpty()) {
                    ttsWrapper.setEngine(s.ttsEnginePackage)
                }
            }
        }

        // Check for session restore - only on fresh launch when no explicit document has been opened
        viewModelScope.launch {
            settingsRepo.sessionDraftFlow.collect { draft ->
                if (draft != null && draft.text.isNotBlank() && _textValue.value.text.isEmpty() && !isExplicitDocumentSession) {
                    _showResumePopup.value = true
                }
            }
        }
    }

    fun keepEditingDraft() {
        val draft = savedDraft.value
        if (draft != null) {
            _textValue.value = TextFieldValue(
                text = draft.text,
                selection = TextRange(
                    draft.selectionStart.coerceIn(0, draft.text.length),
                    draft.selectionEnd.coerceIn(0, draft.text.length)
                )
            )
            _fileName.value = draft.fileName
            if (draft.uriString.isNotBlank()) {
                _currentFileUri.value = Uri.parse(draft.uriString)
            }
            ttsWrapper.speakFeedback("Resumed draft")
        }
        _showResumePopup.value = false
    }

    fun openLastSavedFile() {
        val draft = savedDraft.value
        if (draft != null && draft.uriString.isNotBlank()) {
            loadFromUri(Uri.parse(draft.uriString))
        }
        _showResumePopup.value = false
    }

    fun startNewDocument() {
        newFile()
        viewModelScope.launch {
            settingsRepo.clearSessionDraft()
        }
        _showResumePopup.value = false
    }

    fun dismissResumePopup() {
        _showResumePopup.value = false
    }

    fun onTextChanged(newValue: TextFieldValue) {
        if (_textValue.value == newValue) return
        val oldVal = _textValue.value
        val oldText = oldVal.text
        if (newValue.text != oldText && !isPerformingUndoRedo) {
            recordSnapshot(oldVal)
        }
        _textValue.value = newValue
        if (newValue.text != oldText) {
            cachedHeadings = null // Invalidate session cache on document edit
            if (_selActive.value) {
                _selActive.value = false
            }
            selAnchor = null
            idealX = null
            _transientHighlightRange.value = null
            persistDraft()
        } else {
            // Text is unchanged - cursor moved or text was selected via touch
            if (newValue.selection.start != newValue.selection.end) {
                _selActive.value = true
                selAnchor = newValue.selection.start
            }
        }
    }

    private fun recordSnapshot(snapshot: TextFieldValue) {
        undoStack.addLast(snapshot)
        if (undoStack.size > 100) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isEmpty()) {
            ttsWrapper.speakFeedback("Nothing to undo")
            return
        }
        val current = _textValue.value
        redoStack.addLast(current)
        val prev = undoStack.removeLast()
        isPerformingUndoRedo = true
        _textValue.value = prev
        cachedHeadings = null
        resetCursorState()
        persistDraft()
        isPerformingUndoRedo = false
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        ttsWrapper.speakFeedback("Undo")
    }

    fun redo() {
        if (redoStack.isEmpty()) {
            ttsWrapper.speakFeedback("Nothing to redo")
            return
        }
        val current = _textValue.value
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        isPerformingUndoRedo = true
        _textValue.value = next
        cachedHeadings = null
        resetCursorState()
        persistDraft()
        isPerformingUndoRedo = false
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        ttsWrapper.speakFeedback("Redo")
    }

    // Toggle K without resetting active selection
    fun toggleK() { 
        val newVal = !_kActive.value
        _kActive.value = newVal
        speakButtonFeedback("K", if (newVal) "Character mode" else "Word mode")
    }

    // Toggle P without resetting active selection
    fun toggleP() { 
        val newVal = !_pActive.value
        _pActive.value = newVal
        speakButtonFeedback("P", if (newVal) "Paragraph mode" else "Line mode")
    }
    
    fun toggleSel() { 
        val current = _textValue.value
        val hasText = current.text.isNotEmpty()
        val hasSelection = current.selection.start != current.selection.end

        if (_selActive.value) {
            // Turn off selection mode and collapse selection to current cursor
            _selActive.value = false
            selAnchor = null
            val caret = current.selection.end.coerceIn(0, current.text.length)
            _textValue.value = current.copy(
                selection = TextRange(caret, caret),
                composition = null
            )
            _transientHighlightRange.value = null
            speakButtonFeedback("SEL", "Selection mode off")
        } else {
            // Turn on selection mode
            _selActive.value = true
            _transientHighlightRange.value = null
            if (hasSelection) {
                // Keep current selection and set anchor to the starting point
                selAnchor = current.selection.start
                speakButtonFeedback("SEL", "Selection mode on")
            } else {
                // Lock anchor at current caret position
                val caret = current.selection.end.coerceIn(0, current.text.length)
                selAnchor = caret
                _textValue.value = current.copy(
                    selection = TextRange(caret, caret),
                    composition = null
                )
                speakButtonFeedback("SEL", "Selection mode on")
            }
        }
    }
    
    fun toggleKbLock() { 
        val newVal = !_kbLockActive.value
        _kbLockActive.value = newVal
        speakButtonFeedback("KB_LOCK", if (newVal) "Keyboard locked" else "Keyboard unlocked")
    }

    fun openLanguagePicker() {
        _showLanguagePicker.value = true
    }

    fun closeLanguagePicker() {
        _showLanguagePicker.value = false
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateVoiceLanguage(langCode)
            settingsRepo.updateTtsLanguage(langCode)
            ttsWrapper.setLanguage(langCode)
            closeLanguagePicker()
            ttsWrapper.speakFeedback("Voice and read language set to $langCode")
        }
    }

    fun updateTtsLanguage(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsLanguage(langCode)
            ttsWrapper.setLanguage(langCode)
            ttsWrapper.speakFeedback("TTS set to $langCode")
        }
    }

    fun updateTtsEngine(enginePkg: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsEnginePackage(enginePkg)
            ttsWrapper.setEngine(enginePkg)
            ttsWrapper.speakFeedback("TTS engine updated")
        }
    }

    fun updateSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepo.updateTtsSpeed(speed)
            ttsWrapper.setSpeechRate(speed)
        }
    }

    fun updatePitch(pitch: Float) {
        viewModelScope.launch {
            settingsRepo.updateTtsPitch(pitch)
            ttsWrapper.setPitch(pitch)
        }
    }

    fun updateTheme(themeName: String, bgHex: Long, textHex: Long, hlHex: Long) {
        viewModelScope.launch {
            settingsRepo.updateTheme(themeName, bgHex, textHex, hlHex)
        }
    }

    fun updateHighlightColor(hex: Long) {
        viewModelScope.launch {
            settingsRepo.updateHighlightColor(hex)
        }
    }

    fun updateBackgroundColor(hex: Long) {
        viewModelScope.launch {
            settingsRepo.updateBackgroundColor(hex)
        }
    }

    fun updateTextColor(hex: Long) {
        viewModelScope.launch {
            settingsRepo.updateTextColor(hex)
        }
    }

    fun updateTextSize(sizeSp: Float) {
        viewModelScope.launch {
            settingsRepo.updateTextSize(sizeSp)
        }
    }

    fun updateHideHeadingSymbols(hide: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateHideHeadingSymbols(hide)
        }
    }

    fun openJumpTo() {
        // Scan document only on first tap for the current open file session
        if (cachedHeadings == null) {
            cachedHeadings = HeadingLogic.scanHeadings(_textValue.value.text)
        }
        _jumpToHeadings.value = cachedHeadings ?: emptyList()
        _showJumpToDialog.value = true
    }

    fun closeJumpTo() {
        _showJumpToDialog.value = false
    }

    fun jumpToHeading(item: HeadingItem) {
        val docText = _textValue.value.text
        val targetPos = item.textStartOffset.coerceIn(0, docText.length)
        _textValue.value = _textValue.value.copy(
            selection = TextRange(targetPos, targetPos),
            composition = null
        )
        _selActive.value = false
        selAnchor = null
        idealX = null
        _transientHighlightRange.value = CursorLogic.getWordRangeAt(docText, targetPos)
        _showJumpToDialog.value = false
        ttsWrapper.speakFeedback("Jumped to ${item.headingText}")
    }

    fun updateButtonOrder(order: List<ActionButton>) {
        viewModelScope.launch {
            settingsRepo.updateButtonOrder(order)
        }
    }

    fun updateAdvancedSettingsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateAdvancedSettingsEnabled(enabled)
        }
    }

    fun toggleSpeechFeedbackForButton(buttonName: String) {
        viewModelScope.launch {
            settingsRepo.toggleSpeechFeedbackForButton(buttonName)
        }
    }

    fun updateDisabledSpeechFeedbackButtons(disabledList: List<String>) {
        viewModelScope.launch {
            settingsRepo.updateDisabledSpeechFeedbackButtons(disabledList)
        }
    }

    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateHapticFeedbackEnabled(enabled)
        }
    }

    fun isSpeechFeedbackEnabledFor(buttonName: String): Boolean {
        val s = settings.value
        if (!s.advancedSettingsEnabled) return true
        val disabled = s.disabledSpeechFeedbackButtons.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        return !disabled.contains(buttonName.uppercase())
    }

    fun speakButtonFeedback(buttonName: String, phrase: String) {
        if (isSpeechFeedbackEnabledFor(buttonName)) {
            ttsWrapper.speakFeedback(phrase)
        }
    }

    fun updateButtonSizeMultiplier(multiplier: Float) {
        viewModelScope.launch {
            settingsRepo.updateButtonSizeMultiplier(multiplier)
        }
    }

    fun updateArrowSize(scale: Float) {
        viewModelScope.launch {
            settingsRepo.updateArrowSize(scale)
        }
    }

    fun updateAlwaysInsertMicDirectly(always: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateAlwaysInsertMicDirectly(always)
        }
    }

    fun setCaretFromTap(offset: Int) {
        val text = _textValue.value.text
        val safeOffset = offset.coerceIn(0, text.length)
        _textValue.value = _textValue.value.copy(
            selection = TextRange(safeOffset, safeOffset),
            composition = null
        )
        _selActive.value = false
        selAnchor = null
        idealX = null
        _transientHighlightRange.value = null
    }

    fun togglePlay() {
        if (ttsWrapper.isPlaying.value) {
            ttsWrapper.stop()
        } else {
            val text = _textValue.value.text
            if (text.isNotBlank()) {
                val start = _textValue.value.selection.min
                val textToRead = if (_textValue.value.selection.length > 0) {
                    text.substring(_textValue.value.selection.min, _textValue.value.selection.max)
                } else {
                    text.substring(start)
                }
                // Strip leading heading square symbols so TTS never reads them aloud
                val sanitizedTextToRead = HeadingLogic.stripHeadingSymbolsForTTS(textToRead)
                ttsWrapper.play(sanitizedTextToRead, 0)
            } else {
                ttsWrapper.speakFeedback("Document is empty")
            }
        }
    }

    fun selectAll() {
        val len = _textValue.value.text.length
        if (len > 0) {
            _selActive.value = true
            selAnchor = 0
            _textValue.value = _textValue.value.copy(
                selection = TextRange(0, len),
                composition = null
            )
            _transientHighlightRange.value = null
            idealX = null
            speakButtonFeedback("SELECT_ALL", "Selected all")
        } else {
            speakButtonFeedback("SELECT_ALL", "Document is empty")
        }
    }

    fun loadFromUri(uri: Uri, isFromExternalOrExplicitOpen: Boolean = true) {
        if (isFromExternalOrExplicitOpen) {
            isExplicitDocumentSession = true
            _showResumePopup.value = false
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                val resolvedName = getFileName(context.contentResolver, uri) ?: "document.txt"
                val fileType = com.example.logic.SupportedFileType.fromFileName(resolvedName)

                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    com.example.logic.DocumentFileHandler.readDocument(stream, fileType)
                } ?: ""
                
                cachedHeadings = null
                undoStack.clear()
                redoStack.clear()
                _canUndo.value = false
                _canRedo.value = false
                _textValue.value = TextFieldValue(text = text, selection = TextRange(0, 0))
                _currentFileUri.value = uri
                _fileName.value = resolvedName
                resetCursorState()
                ttsWrapper.speakFeedback("Opened $resolvedName")
            } catch (e: Exception) {
                e.printStackTrace()
                ttsWrapper.speakFeedback("Error opening file")
            }
        }
    }

    fun saveToUri(uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                val resolvedName = getFileName(context.contentResolver, uri) ?: _fileName.value
                val fileType = com.example.logic.SupportedFileType.fromFileName(resolvedName)

                // For docx overwrite, attempt to read existing bytes to preserve assets and styles
                val existingBytes = if (fileType == com.example.logic.SupportedFileType.DOCX) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        null
                    }
                } else null

                context.contentResolver.openOutputStream(uri, "rwt")?.use { outputStream ->
                    val existingStream = existingBytes?.let { java.io.ByteArrayInputStream(it) }
                    com.example.logic.DocumentFileHandler.writeDocument(
                        text = _textValue.value.text,
                        outputStream = outputStream,
                        fileType = fileType,
                        existingInputStream = existingStream
                    )
                    _currentFileUri.value = uri
                    _fileName.value = resolvedName
                    ttsWrapper.speakFeedback("File saved")
                } ?: run {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val existingStream = existingBytes?.let { java.io.ByteArrayInputStream(it) }
                        com.example.logic.DocumentFileHandler.writeDocument(
                            text = _textValue.value.text,
                            outputStream = outputStream,
                            fileType = fileType,
                            existingInputStream = existingStream
                        )
                        _currentFileUri.value = uri
                        _fileName.value = resolvedName
                        ttsWrapper.speakFeedback("File saved")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ttsWrapper.speakFeedback("Error saving file")
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
                val fileType = com.example.logic.SupportedFileType.fromFileName(file.name)
                val existingBytes = if (file.exists() && fileType == com.example.logic.SupportedFileType.DOCX) {
                    try { file.readBytes() } catch (e: Exception) { null }
                } else null

                java.io.FileOutputStream(file).use { out ->
                    val existingStream = existingBytes?.let { java.io.ByteArrayInputStream(it) }
                    com.example.logic.DocumentFileHandler.writeDocument(
                        text = _textValue.value.text,
                        outputStream = out,
                        fileType = fileType,
                        existingInputStream = existingStream
                    )
                }

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
        // Ensure proper extension exists
        val finalFileName = if (rawName.contains('.')) rawName else "$rawName.txt"
        val fileType = com.example.logic.SupportedFileType.fromFileName(finalFileName)

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
                        fileType.mimeType,
                        finalFileName
                    )
                    if (newFileUri != null) {
                        context.contentResolver.openOutputStream(newFileUri)?.use { out ->
                            com.example.logic.DocumentFileHandler.writeDocument(
                                text = _textValue.value.text,
                                outputStream = out,
                                fileType = fileType
                            )
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
                    val existingBytes = if (file.exists() && fileType == com.example.logic.SupportedFileType.DOCX) {
                        try { file.readBytes() } catch (e: Exception) { null }
                    } else null

                    java.io.FileOutputStream(file).use { out ->
                        val existingStream = existingBytes?.let { java.io.ByteArrayInputStream(it) }
                        com.example.logic.DocumentFileHandler.writeDocument(
                            text = _textValue.value.text,
                            outputStream = out,
                            fileType = fileType,
                            existingInputStream = existingStream
                        )
                    }
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
        isExplicitDocumentSession = true
        _showResumePopup.value = false
        cachedHeadings = null
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _textValue.value = TextFieldValue("")
        _currentFileUri.value = null
        _fileName.value = "newfile.txt"
        resetCursorState()
        ttsWrapper.speakFeedback("New file")
    }

    private fun getFileName(contentResolver: android.content.ContentResolver, uri: Uri): String? {
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
        recordSnapshot(current)
        val start = current.selection.min
        val end = current.selection.max
        val newString = current.text.substring(0, start) + newText + current.text.substring(end)
        _textValue.value = current.copy(text = newString, selection = TextRange(start + newText.length), composition = null)
        cachedHeadings = null
        resetCursorState()
        persistDraft()
        closeReplacePopup()
        if (newText.isNotBlank()) {
            ttsWrapper.speakFeedback(newText)
        } else {
            ttsWrapper.speakFeedback("Replaced text")
        }
    }

    private fun insertTextAtCursor(textToInsert: String) {
        val current = _textValue.value
        recordSnapshot(current)
        val start = current.selection.min
        val end = current.selection.max
        val newString = current.text.substring(0, start) + textToInsert + current.text.substring(end)
        _textValue.value = current.copy(text = newString, selection = TextRange(start + textToInsert.length), composition = null)
        cachedHeadings = null
        resetCursorState()
        persistDraft()
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
                            ':' -> "Colon"
                            ';' -> "Semicolon"
                            '-' -> "Hyphen"
                            '(' -> "Open parenthesis"
                            ')' -> "Close parenthesis"
                            '\"' -> "Quote"
                            '\'' -> "Apostrophe"
                            '/' -> "Slash"
                            '\\' -> "Backslash"
                            '@' -> "At sign"
                            '#' -> "Hash"
                            '$' -> "Dollar"
                            '%' -> "Percent"
                            '&' -> "Ampersand"
                            '*' -> "Asterisk"
                            '+' -> "Plus"
                            '=' -> "Equals"
                            '<' -> "Less than"
                            '>' -> "Greater than"
                            else -> ch.toString()
                        }
                        ttsWrapper.speakFeedback(spoken)
                    }
                } else {
                    val wordRange = CursorLogic.getWordRangeAt(text, if (direction == ArrowDirection.LEFT) newCaret else (newCaret - 1).coerceAtLeast(0))
                    val word = if (wordRange.start < wordRange.end && wordRange.end <= text.length) {
                        text.substring(wordRange.start, wordRange.end).trim()
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
                    val cleanPText = HeadingLogic.stripLeadingSymbols(pText)
                    if (cleanPText.isNotBlank()) {
                        val sample = if (cleanPText.length > 60) cleanPText.substring(0, 60) + "..." else cleanPText
                        ttsWrapper.speakFeedback(sample)
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
                    val cleanLineText = HeadingLogic.stripLeadingSymbols(lineText)
                    if (cleanLineText.isNotBlank()) {
                        ttsWrapper.speakFeedback(cleanLineText)
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
        speakButtonFeedback("TOP", "Top of document")
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
        speakButtonFeedback("END", "End of document")
    }

    fun onAction(action: ActionButton) {
        when (action) {
            ActionButton.K -> toggleK()
            ActionButton.P -> toggleP()
            ActionButton.KB_LOCK -> toggleKbLock()
            ActionButton.SELECT_ALL -> selectAll()
            ActionButton.JUMP_TO -> openJumpTo()
            ActionButton.TOP -> jumpStart()
            ActionButton.END -> jumpEnd()
            else -> {
                val current = _textValue.value
                val hasSelection = current.selection.start != current.selection.end
                val clipboardText = if (action == ActionButton.PASTE) pasteFromClipboard() else null
                
                val result = com.example.logic.TextActionLogic.handleAction(
                    action = action,
                    currentValue = current,
                    clipboardText = clipboardText,
                    onCopy = { copyToClipboard(it) }
                )
                if (result.text != current.text) {
                    recordSnapshot(current)
                }
                _textValue.value = result
                persistDraft()

                when (action) {
                    ActionButton.CUT -> {
                        if (hasSelection) {
                            speakButtonFeedback("CUT", "Cut")
                        } else {
                            speakButtonFeedback("CUT", "Nothing selected to cut")
                        }
                    }
                    ActionButton.COPY -> {
                        if (hasSelection) {
                            speakButtonFeedback("COPY", "Copied")
                        } else {
                            speakButtonFeedback("COPY", "Nothing selected to copy")
                        }
                    }
                    ActionButton.DELETE -> {
                        speakButtonFeedback("DELETE", "Deleted")
                    }
                    ActionButton.PASTE -> {
                        if (!clipboardText.isNullOrEmpty()) {
                            speakButtonFeedback("PASTE", "Pasted")
                        } else {
                            speakButtonFeedback("PASTE", "Clipboard is empty")
                        }
                    }
                    ActionButton.ENTER -> speakButtonFeedback("ENTER", "Enter")
                    else -> {}
                }
                
                resetCursorState()
            }
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("VoiceType", text)
        clipboard.setPrimaryClip(clip)
    }

    fun pasteFromClipboard(): String {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip ?: return ""
            if (clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                // coerceToText preserves text and newline formatting across plain text, styled text, and HTML/URIs
                val rawCharSequence = item.coerceToText(context)
                val rawText = rawCharSequence?.toString() ?: ""
                // Normalize Windows CRLF (\r\n) or legacy Mac CR (\r) into standard \n so all paragraph breaks survive
                return rawText.replace("\r\n", "\n").replace("\r", "\n")
            }
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

    private fun persistDraft() {
        viewModelScope.launch {
            val cur = _textValue.value
            settingsRepo.saveSessionDraft(
                SessionDraft(
                    text = cur.text,
                    fileName = _fileName.value,
                    uriString = _currentFileUri.value?.toString() ?: "",
                    selectionStart = cur.selection.start,
                    selectionEnd = cur.selection.end
                )
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsWrapper.shutdown()
    }
}
