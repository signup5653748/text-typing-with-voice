package com.example.presentation.editor

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.editor.DocumentFileManager
import com.example.core.editor.EditorSelectionManager
import com.example.core.editor.EditorSpeechManager
import com.example.data.ActionButton
import com.example.data.SessionDraft
import com.example.data.SettingsEntity
import com.example.data.SettingsRepository
import com.example.data.StarredFolder
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.logic.HeadingItem
import com.example.logic.HeadingLogic
import com.example.logic.TextActionLogic
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TTSWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-level coordinator ViewModel for the VoiceType Editor screen.
 * Delegates document file I/O, speech synthesis/recognition, and selection logic
 * to focused managers.
 */
open class EditorViewModel(
    application: Application,
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, SettingsRepository(application))

    val ttsWrapper = TTSWrapper(application)
    val speechWrapper = SpeechRecognitionWrapper(application)

    private val fileManager = DocumentFileManager(application, settingsRepo)
    private val speechManager = EditorSpeechManager(ttsWrapper, viewModelScope)
    private val selectionManager = EditorSelectionManager()

    // Primary document state
    private val _textValue = MutableStateFlow(TextFieldValue(""))
    val textValue: StateFlow<TextFieldValue> = _textValue.asStateFlow()

    private val _currentFileUri = MutableStateFlow<Uri?>(null)
    val currentFileUri: StateFlow<Uri?> = _currentFileUri.asStateFlow()

    private val _fileName = MutableStateFlow("newfile.txt")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    // Mode flags
    private val _kActive = MutableStateFlow(false)
    val kActive: StateFlow<Boolean> = _kActive.asStateFlow()

    private val _pActive = MutableStateFlow(false)
    val pActive: StateFlow<Boolean> = _pActive.asStateFlow()

    private val _selActive = MutableStateFlow(false)
    val selActive: StateFlow<Boolean> = _selActive.asStateFlow()

    private val _kbLockActive = MutableStateFlow(false)
    val kbLockActive: StateFlow<Boolean> = _kbLockActive.asStateFlow()

    // Undo / Redo
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    private var isPerformingUndoRedo = false

    // Dialog & popup visibility
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

    // Visual highlights
    private val _transientHighlightRange = MutableStateFlow<TextRange?>(null)
    val transientHighlightRange: StateFlow<TextRange?> = _transientHighlightRange.asStateFlow()
    val speechHighlightRange: StateFlow<TextRange?> = speechManager.speechHighlightRange

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<IntRange>>(emptyList())
    val searchMatches: StateFlow<List<IntRange>> = _searchMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()
    private var searchJob: Job? = null

    // Jump to state & cache
    private var cachedHeadings: List<HeadingItem>? = null
    private val _showJumpToDialog = MutableStateFlow(false)
    val showJumpToDialog: StateFlow<Boolean> = _showJumpToDialog.asStateFlow()

    private val _jumpToHeadings = MutableStateFlow<List<HeadingItem>>(emptyList())
    val jumpToHeadings: StateFlow<List<HeadingItem>> = _jumpToHeadings.asStateFlow()

    private var isExplicitDocumentSession = false
    private var draftPersistJob: Job? = null

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

    init {
        // Collect voice dictation results
        viewModelScope.launch {
            speechWrapper.finalResult.collect { result ->
                if (!result.isNullOrBlank()) {
                    if (!_showReplacePopup.value) {
                        insertTextAtCursor(result)
                        ttsWrapper.speakFeedback(result)
                    }
                }
            }
        }

        // Apply settings to TTS engine on start
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                ttsWrapper.setSpeechRate(s.ttsSpeed)
                ttsWrapper.setPitch(s.ttsPitch)
                if (s.ttsLanguage.isNotBlank()) {
                    ttsWrapper.setLanguage(s.ttsLanguage)
                }
                if (s.ttsVoiceName.isNotBlank()) {
                    ttsWrapper.setVoice(s.ttsVoiceName)
                }
                if (s.ttsEnginePackage.isNotBlank()) {
                    ttsWrapper.setEngine(s.ttsEnginePackage)
                }
            }
        }

        // Check for session draft restore on fresh launch
        viewModelScope.launch {
            settingsRepo.sessionDraftFlow.collect { draft ->
                if (draft != null && draft.text.isNotBlank() && _textValue.value.text.isEmpty() && !isExplicitDocumentSession) {
                    _showResumePopup.value = true
                }
            }
        }
    }

    // --- Text Change & History ---

    fun onTextChanged(newValue: TextFieldValue) {
        if (_textValue.value == newValue) return
        val oldVal = _textValue.value
        val oldText = oldVal.text
        if (newValue.text != oldText && !isPerformingUndoRedo) {
            recordSnapshot(oldVal)
        }
        _textValue.value = newValue
        if (newValue.text != oldText) {
            speechManager.stopPlayback()
            cachedHeadings = null
            if (_selActive.value) {
                _selActive.value = false
            }
            selectionManager.resetCursorState(newValue)
            _transientHighlightRange.value = null
            persistDraft()
        } else {
            if (newValue.selection.start != newValue.selection.end) {
                _selActive.value = true
                selectionManager.selAnchor = newValue.selection.start
            }
        }
    }

    private fun recordSnapshot(snapshot: TextFieldValue) {
        undoStack.addLast(snapshot)
        if (undoStack.size > 100) undoStack.removeFirst()
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

    // --- Mode Toggles ---

    fun toggleK() {
        val newVal = !_kActive.value
        _kActive.value = newVal
        speakButtonFeedback("K", if (newVal) "Character mode" else "Word mode")
    }

    fun toggleP() {
        val newVal = !_pActive.value
        _pActive.value = newVal
        speakButtonFeedback("P", if (newVal) "Paragraph mode" else "Line mode")
    }

    fun toggleSel() {
        val current = _textValue.value
        val hasSelection = current.selection.start != current.selection.end

        if (_selActive.value) {
            _selActive.value = false
            selectionManager.selAnchor = null
            val caret = current.selection.end.coerceIn(0, current.text.length)
            _textValue.value = current.copy(selection = TextRange(caret, caret), composition = null)
            _transientHighlightRange.value = null
            speakButtonFeedback("SEL", "Selection mode off")
        } else {
            _selActive.value = true
            _transientHighlightRange.value = null
            if (hasSelection) {
                selectionManager.selAnchor = current.selection.start
            } else {
                val caret = current.selection.end.coerceIn(0, current.text.length)
                selectionManager.selAnchor = caret
                _textValue.value = current.copy(selection = TextRange(caret, caret), composition = null)
            }
            speakButtonFeedback("SEL", "Selection mode on")
        }
    }

    fun toggleKbLock() {
        val newVal = !_kbLockActive.value
        _kbLockActive.value = newVal
        speakButtonFeedback("KB_LOCK", if (newVal) "Keyboard locked" else "Keyboard unlocked")
    }

    fun setCaretFromTap(offset: Int) {
        val text = _textValue.value.text
        val safeOffset = offset.coerceIn(0, text.length)
        _textValue.value = _textValue.value.copy(selection = TextRange(safeOffset, safeOffset), composition = null)
        resetCursorState()
    }

    // --- Directional Navigation ---

    private fun handleArrow(direction: ArrowDirection, layoutResult: TextLayoutResult? = null) {
        val result = selectionManager.handleArrow(
            value = _textValue.value,
            direction = direction,
            isKActive = _kActive.value,
            isPActive = _pActive.value,
            isSelActive = _selActive.value,
            layoutResult = layoutResult
        )
        _textValue.value = result.newValue
        _transientHighlightRange.value = result.transientHighlightRange
        result.feedbackToSpeak?.let { ttsWrapper.speakFeedback(it) }
    }

    fun moveLeft() = handleArrow(ArrowDirection.LEFT)
    fun moveRight() = handleArrow(ArrowDirection.RIGHT)
    fun moveUp(layoutResult: TextLayoutResult?) = handleArrow(ArrowDirection.UP, layoutResult)
    fun moveDown(layoutResult: TextLayoutResult?) = handleArrow(ArrowDirection.DOWN, layoutResult)

    fun jumpStart() {
        val end = 0
        if (_selActive.value) {
            val anchor = selectionManager.selAnchor ?: _textValue.value.selection.end
            selectionManager.selAnchor = anchor
            _textValue.value = _textValue.value.copy(selection = TextRange(anchor, end), composition = null)
            _transientHighlightRange.value = null
        } else {
            _textValue.value = _textValue.value.copy(selection = TextRange(end, end), composition = null)
            _transientHighlightRange.value = CursorLogic.getWordRangeAt(_textValue.value.text, 0)
        }
        selectionManager.idealX = null
        speakButtonFeedback("TOP", "Top of document")
    }

    fun jumpEnd() {
        val end = _textValue.value.text.length
        if (_selActive.value) {
            val anchor = selectionManager.selAnchor ?: _textValue.value.selection.end
            selectionManager.selAnchor = anchor
            _textValue.value = _textValue.value.copy(selection = TextRange(anchor, end), composition = null)
            _transientHighlightRange.value = null
        } else {
            _textValue.value = _textValue.value.copy(selection = TextRange(end, end), composition = null)
            _transientHighlightRange.value = CursorLogic.getWordRangeAt(_textValue.value.text, if (end > 0) end - 1 else 0)
        }
        selectionManager.idealX = null
        speakButtonFeedback("END", "End of document")
    }

    // --- Action Button Handlers ---

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
                val transient = _transientHighlightRange.value
                val isWord = !_kActive.value

                val result = TextActionLogic.handleAction(
                    action = action,
                    currentValue = current,
                    transientRange = transient,
                    isWordMode = isWord,
                    clipboardText = clipboardText,
                    onCopy = { copyToClipboard(it) }
                )
                if (result.text != current.text) {
                    recordSnapshot(current)
                }
                _textValue.value = result
                _transientHighlightRange.value = null
                persistDraft()

                when (action) {
                    ActionButton.CUT -> speakButtonFeedback("CUT", if (hasSelection || transient != null) "Cut" else "Nothing selected to cut")
                    ActionButton.COPY -> speakButtonFeedback("COPY", if (hasSelection || transient != null) "Copied" else "Nothing selected to copy")
                    ActionButton.DELETE -> speakButtonFeedback("DELETE", "Deleted")
                    ActionButton.PASTE -> speakButtonFeedback("PASTE", if (!clipboardText.isNullOrEmpty()) "Pasted" else "Clipboard is empty")
                    ActionButton.ENTER -> speakButtonFeedback("ENTER", "Enter")
                    else -> {}
                }
                resetCursorState()
            }
        }
    }

    fun selectAll() {
        val len = _textValue.value.text.length
        if (len > 0) {
            _selActive.value = true
            selectionManager.selAnchor = 0
            _textValue.value = _textValue.value.copy(selection = TextRange(0, len), composition = null)
            _transientHighlightRange.value = null
            selectionManager.idealX = null
            speakButtonFeedback("SELECT_ALL", "Selected all")
        } else {
            speakButtonFeedback("SELECT_ALL", "Document is empty")
        }
    }

    fun setSelectionRange(range: TextRange) {
        val current = _textValue.value
        val docLen = current.text.length
        val safeStart = range.start.coerceIn(0, docLen)
        val safeEnd = range.end.coerceIn(0, docLen)
        if (current.selection.start == safeStart && current.selection.end == safeEnd && _transientHighlightRange.value == null) {
            return
        }
        _textValue.value = current.copy(selection = TextRange(safeStart, safeEnd), composition = null)
        _selActive.value = safeStart != safeEnd
        _transientHighlightRange.value = null
    }

    fun clearSelection() {
        val end = _textValue.value.selection.end.coerceIn(0, _textValue.value.text.length)
        _textValue.value = _textValue.value.copy(selection = TextRange(end, end), composition = null)
        _selActive.value = false
        selectionManager.selAnchor = null
        _transientHighlightRange.value = null
    }

    fun addNewLine() = onAction(ActionButton.ENTER)
    fun deleteSelection() = onAction(ActionButton.DELETE)

    /**
     * Standalone delete operation for ranges (e.g. from Reading Mode),
     * without overwriting or coupling with the Editor screen's selection.
     */
    fun deleteRange(range: TextRange) {
        val current = _textValue.value
        if (range.length <= 0) return
        recordSnapshot(current)
        val start = range.min.coerceIn(0, current.text.length)
        val end = range.max.coerceIn(0, current.text.length)
        val newString = current.text.substring(0, start) + current.text.substring(end)
        val homeSelStart = current.selection.start.coerceIn(0, newString.length)
        val homeSelEnd = current.selection.end.coerceIn(0, newString.length)
        _textValue.value = current.copy(
            text = newString,
            selection = TextRange(homeSelStart, homeSelEnd),
            composition = null
        )
        cachedHeadings = null
        persistDraft()
    }

    /**
     * Standalone replace operation for ranges (e.g. from Reading Mode),
     * preserving Editor screen's independent selection state.
     */
    fun replaceRange(range: TextRange, newText: String) {
        val current = _textValue.value
        recordSnapshot(current)
        val start = range.min.coerceIn(0, current.text.length)
        val end = range.max.coerceIn(0, current.text.length)
        val newString = current.text.substring(0, start) + newText + current.text.substring(end)
        val homeSelStart = current.selection.start.coerceIn(0, newString.length)
        val homeSelEnd = current.selection.end.coerceIn(0, newString.length)
        _textValue.value = current.copy(
            text = newString,
            selection = TextRange(homeSelStart, homeSelEnd),
            composition = null
        )
        cachedHeadings = null
        persistDraft()
    }

    /**
     * Standalone speech playback from Reading Mode's independent selection/cursor.
     */
    fun playReadingSelection(range: TextRange) {
        val docText = _textValue.value.text
        if (docText.isBlank()) {
            ttsWrapper.speakFeedback("Document is empty")
            return
        }
        if (range.length > 0) {
            playFrom(range.min, range.max)
        } else {
            val start = range.min.coerceIn(0, docText.length)
            if (start < docText.length && docText.substring(start).isNotBlank()) {
                playFrom(start, docText.length)
            } else {
                playFrom(0, docText.length)
            }
        }
    }

    // --- Audio Playback & Voice ---

    fun stopPlayback() = speechManager.stopPlayback()

    fun playFrom(startOffset: Int = 0, endOffset: Int? = null) {
        speechManager.playFrom(
            docText = _textValue.value.text,
            startOffset = startOffset,
            endOffset = endOffset,
            ttsSpeed = settings.value.ttsSpeed,
            highlightUnit = settings.value.highlightUnit
        )
    }

    fun playReadingModeFromTop() {
        val docText = _textValue.value.text
        if (docText.isBlank()) {
            ttsWrapper.speakFeedback("Document is empty")
            return
        }
        playFrom(0, docText.length)
    }

    fun togglePlay() {
        if (ttsWrapper.isPlaying.value) {
            stopPlayback()
        } else {
            val docText = _textValue.value.text
            if (docText.isNotBlank()) {
                val hasExplicitSelection = _textValue.value.selection.length > 0
                val (readStart, readEnd) = if (hasExplicitSelection) {
                    Pair(_textValue.value.selection.min, _textValue.value.selection.max)
                } else {
                    val cursor = _textValue.value.selection.min
                    if (cursor < docText.length && docText.substring(cursor).isNotBlank()) {
                        Pair(cursor, docText.length)
                    } else {
                        Pair(0, docText.length)
                    }
                }
                playFrom(readStart, readEnd)
            } else {
                ttsWrapper.speakFeedback("Document is empty")
            }
        }
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
        _showReplacePopup.value = true
        speechWrapper.clearResults()
        speechWrapper.startListening(settings.value.voiceLanguage)
    }

    fun closeReplacePopup() {
        _showReplacePopup.value = false
        speechWrapper.stopListening()
        speechWrapper.clearResults()
    }

    fun getSelectedText(): String {
        val current = _textValue.value
        val sel = current.selection
        return if (sel.length > 0 && sel.max <= current.text.length) {
            current.text.substring(sel.min, sel.max)
        } else ""
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
        ttsWrapper.speakFeedback(if (newText.isNotBlank()) newText else "Replaced text")
    }

    fun applyReplace(findText: String, newText: String) {
        val current = _textValue.value
        recordSnapshot(current)
        val currentSel = current.selection
        val newString: String
        val newCaret: Int

        if (currentSel.length > 0 && current.text.substring(currentSel.min, currentSel.max) == findText) {
            newString = current.text.substring(0, currentSel.min) + newText + current.text.substring(currentSel.max)
            newCaret = currentSel.min + newText.length
        } else if (findText.isNotEmpty()) {
            val idx = current.text.indexOf(findText, startIndex = currentSel.end.coerceIn(0, current.text.length))
                .let { if (it >= 0) it else current.text.indexOf(findText) }
            if (idx >= 0) {
                newString = current.text.substring(0, idx) + newText + current.text.substring(idx + findText.length)
                newCaret = idx + newText.length
            } else {
                newString = current.text.substring(0, currentSel.min) + newText + current.text.substring(currentSel.max)
                newCaret = currentSel.min + newText.length
            }
        } else {
            newString = current.text.substring(0, currentSel.min) + newText + current.text.substring(currentSel.max)
            newCaret = currentSel.min + newText.length
        }

        _textValue.value = current.copy(text = newString, selection = TextRange(newCaret), composition = null)
        cachedHeadings = null
        resetCursorState()
        persistDraft()
        closeReplacePopup()
        ttsWrapper.speakFeedback(if (newText.isNotBlank()) "Replaced" else "Deleted")
    }

    fun applyReplaceAll(findText: String, newText: String) {
        if (findText.isEmpty()) {
            applyReplace(newText)
            return
        }
        val current = _textValue.value
        recordSnapshot(current)
        val count = current.text.split(findText).size - 1
        val newString = current.text.replace(findText, newText)
        _textValue.value = current.copy(text = newString, selection = TextRange(0), composition = null)
        cachedHeadings = null
        resetCursorState()
        persistDraft()
        closeReplacePopup()
        ttsWrapper.speakFeedback("Replaced $count occurrences")
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

    // --- Document I/O ---

    fun loadFromUri(uri: Uri, isFromExternalOrExplicitOpen: Boolean = true) {
        if (isFromExternalOrExplicitOpen) {
            isExplicitDocumentSession = true
            _showResumePopup.value = false
        }
        viewModelScope.launch {
            try {
                val (resolvedName, text) = fileManager.readDocumentFromUri(uri)
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
        viewModelScope.launch {
            try {
                val resolvedName = fileManager.writeDocumentToUri(uri, _fileName.value, _textValue.value.text)
                _currentFileUri.value = uri
                _fileName.value = resolvedName
                ttsWrapper.speakFeedback("File saved")
            } catch (e: Exception) {
                e.printStackTrace()
                ttsWrapper.speakFeedback("Error saving file")
            }
        }
    }

    fun saveToFile(file: File) {
        viewModelScope.launch {
            try {
                val savedName = fileManager.writeDocumentToFile(file, _textValue.value.text)
                _currentFileUri.value = Uri.fromFile(file)
                _fileName.value = savedName
                closeSaveDialog()
                ttsWrapper.speakFeedback("Saved as $savedName")
            } catch (e: Exception) {
                e.printStackTrace()
                ttsWrapper.speakFeedback("Error saving file")
            }
        }
    }

    fun saveToStarredFolder(folder: StarredFolder, customFileName: String) {
        viewModelScope.launch {
            try {
                val (newUri, finalName) = fileManager.writeDocumentToStarredFolder(
                    folder = folder,
                    customFileName = customFileName,
                    defaultFileName = _fileName.value,
                    text = _textValue.value.text
                )
                _currentFileUri.value = newUri
                _fileName.value = finalName
                closeSaveDialog()
                ttsWrapper.speakFeedback("Saved to ${folder.name}")
            } catch (e: Exception) {
                e.printStackTrace()
                ttsWrapper.speakFeedback("Error saving document")
            }
        }
    }

    fun addStarredFolderFromTreeUri(treeUri: Uri) {
        viewModelScope.launch {
            try {
                val folderName = fileManager.addStarredFolderFromTreeUri(treeUri)
                ttsWrapper.speakFeedback("Starred folder $folderName added")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeStarredFolder(folderId: String) {
        viewModelScope.launch {
            fileManager.removeStarredFolder(folderId)
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

    fun openSaveDialog(isSaveAs: Boolean = false) {
        _isSaveAsMode.value = isSaveAs
        _showSaveDialog.value = true
    }

    fun closeSaveDialog() {
        _showSaveDialog.value = false
    }

    fun saveCurrentFile() {
        val uri = _currentFileUri.value
        if (uri != null) saveToUri(uri) else openSaveDialog(isSaveAs = true)
    }

    // --- Jump to Headings ---

    fun openJumpTo() {
        viewModelScope.launch {
            if (cachedHeadings == null) {
                val currentText = _textValue.value.text
                cachedHeadings = withContext(Dispatchers.Default) {
                    HeadingLogic.scanHeadings(currentText)
                }
            }
            _jumpToHeadings.value = cachedHeadings ?: emptyList()
            _showJumpToDialog.value = true
        }
    }

    fun closeJumpTo() {
        _showJumpToDialog.value = false
    }

    fun jumpToHeading(item: HeadingItem) {
        val docText = _textValue.value.text
        val targetPos = item.textStartOffset.coerceIn(0, docText.length)
        _textValue.value = _textValue.value.copy(selection = TextRange(targetPos, targetPos), composition = null)
        resetCursorState()
        _transientHighlightRange.value = CursorLogic.getWordRangeAt(docText, targetPos)
        _showJumpToDialog.value = false
        ttsWrapper.speakFeedback("Jumped to ${item.headingText}")
    }

    // --- Search ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchMatches.value = emptyList()
            _currentMatchIndex.value = -1
            return
        }

        searchJob = viewModelScope.launch {
            delay(120)
            val docText = _textValue.value.text
            val currentPos = _textValue.value.selection.start
            val matches = withContext(Dispatchers.Default) {
                val list = mutableListOf<IntRange>()
                var index = 0
                while (index < docText.length) {
                    val found = docText.indexOf(query, startIndex = index, ignoreCase = true)
                    if (found >= 0) {
                        list.add(found until (found + query.length))
                        index = found + query.length.coerceAtLeast(1)
                    } else break
                }
                list
            }

            _searchMatches.value = matches
            if (matches.isNotEmpty()) {
                var closestIdx = matches.indexOfFirst { it.first >= currentPos }
                if (closestIdx < 0) closestIdx = 0
                _currentMatchIndex.value = closestIdx
                highlightAndSpeakMatch(matches[closestIdx])
            } else {
                _currentMatchIndex.value = -1
            }
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
        _textValue.value = _textValue.value.copy(selection = TextRange(start, end), composition = null)
        val matchedText = text.substring(start, end)
        if (matchedText.isNotBlank()) ttsWrapper.play(matchedText, 0)
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchMatches.value = emptyList()
        _currentMatchIndex.value = -1
        ttsWrapper.stop()
    }

    // --- Session Draft ---

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
        viewModelScope.launch { fileManager.clearSessionDraft() }
        _showResumePopup.value = false
    }

    fun dismissResumePopup() {
        _showResumePopup.value = false
    }

    private fun persistDraft() {
        draftPersistJob?.cancel()
        draftPersistJob = viewModelScope.launch {
            delay(500)
            val cur = _textValue.value
            fileManager.saveSessionDraft(
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

    // --- Language & Settings Helpers ---

    fun openLanguagePicker() { _showLanguagePicker.value = true }
    fun closeLanguagePicker() { _showLanguagePicker.value = false }

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
            val variants = ttsWrapper.getVoiceVariantsForLanguage(langCode)
            if (variants.isNotEmpty() && variants.none { it.name == settings.value.ttsVoiceName }) {
                val first = variants.firstOrNull { it.isDownloaded } ?: variants.first()
                settingsRepo.updateTtsVoiceName(first.name)
                ttsWrapper.setVoice(first.name)
            }
            ttsWrapper.speakFeedback("TTS set to $langCode")
        }
    }

    fun updateVoiceLanguageOnly(langCode: String) {
        viewModelScope.launch {
            settingsRepo.updateVoiceLanguage(langCode)
            ttsWrapper.speakFeedback("Voice typing set to $langCode")
        }
    }

    fun updateTtsVoiceVariant(voiceName: String) {
        viewModelScope.launch {
            settingsRepo.updateTtsVoiceName(voiceName)
            ttsWrapper.setVoice(voiceName)
            ttsWrapper.speakFeedback("Voice variant selected")
        }
    }

    fun previewVoiceVariant(variant: com.example.speech.TtsVoiceVariant) {
        ttsWrapper.previewVoice(variant)
    }

    fun getVoiceVariantsForLanguage(langCode: String): List<com.example.speech.TtsVoiceVariant> {
        return ttsWrapper.getVoiceVariantsForLanguage(langCode)
    }

    fun openVoiceDownloadSettings(context: android.content.Context) {
        speechWrapper.openVoiceDownloadSettings(context)
    }

    fun downloadSpeechDictionary(langCode: String, context: android.content.Context) {
        speechWrapper.downloadSpeechDictionary(langCode, context)
    }

    fun openTtsInstallSettings(context: android.content.Context) {
        ttsWrapper.openTtsInstallSettings(context)
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
        viewModelScope.launch { settingsRepo.updateTheme(themeName, bgHex, textHex, hlHex) }
    }

    fun updateHighlightColor(hex: Long) { viewModelScope.launch { settingsRepo.updateHighlightColor(hex) } }
    fun updateBackgroundColor(hex: Long) { viewModelScope.launch { settingsRepo.updateBackgroundColor(hex) } }
    fun updateTextColor(hex: Long) { viewModelScope.launch { settingsRepo.updateTextColor(hex) } }
    fun updateTextSize(sizeSp: Float) { viewModelScope.launch { settingsRepo.updateTextSize(sizeSp) } }
    fun updateHideHeadingSymbols(hide: Boolean) { viewModelScope.launch { settingsRepo.updateHideHeadingSymbols(hide) } }
    fun updateButtonOrder(order: List<ActionButton>) { viewModelScope.launch { settingsRepo.updateButtonOrder(order) } }
    fun updateAdvancedSettingsEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepo.updateAdvancedSettingsEnabled(enabled) } }
    fun toggleSpeechFeedbackForButton(buttonName: String) { viewModelScope.launch { settingsRepo.toggleSpeechFeedbackForButton(buttonName) } }
    fun updateDisabledSpeechFeedbackButtons(disabledList: List<String>) { viewModelScope.launch { settingsRepo.updateDisabledSpeechFeedbackButtons(disabledList) } }
    fun updateHapticFeedbackEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepo.updateHapticFeedbackEnabled(enabled) } }
    fun updateButtonSizeMultiplier(multiplier: Float) { viewModelScope.launch { settingsRepo.updateButtonSizeMultiplier(multiplier) } }
    fun updateArrowSize(scale: Float) { viewModelScope.launch { settingsRepo.updateArrowSize(scale) } }
    fun updateAlwaysInsertMicDirectly(always: Boolean) { viewModelScope.launch { settingsRepo.updateAlwaysInsertMicDirectly(always) } }
    fun updateHighlightUnit(unit: String) { viewModelScope.launch { settingsRepo.updateHighlightUnit(unit) } }

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

    // --- Clipboard ---

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
                val rawCharSequence = item.coerceToText(context)
                val rawText = rawCharSequence?.toString() ?: ""
                return rawText.replace("\r\n", "\n").replace("\r", "\n")
            }
        }
        return ""
    }

    private fun resetCursorState() {
        _selActive.value = false
        _transientHighlightRange.value = null
        _textValue.value = selectionManager.resetCursorState(_textValue.value)
    }

    fun updateStartOnReadingScreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateStartOnReadingScreen(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        draftPersistJob?.cancel()
        speechManager.shutdown()
        speechWrapper.release()
    }
}
