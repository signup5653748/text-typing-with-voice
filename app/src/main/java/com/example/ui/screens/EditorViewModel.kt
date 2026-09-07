package com.example.ui.screens

import android.app.Application
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionButton
import com.example.data.ControlElement
import com.example.data.SettingsEntity
import com.example.data.SettingsRepository
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TTSWrapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    
    val speechWrapper = SpeechRecognitionWrapper(application)
    val ttsWrapper = TTSWrapper(application)

    val settings = settingsRepo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsEntity()
    )

    private val initialText = "This is the first predefined line for testing.\n" +
            "This is the second line with some more text.\n" +
            "\n" +
            "And this is the fourth line after a blank line to complete the setup."

    private val _textValue = MutableStateFlow(TextFieldValue(initialText))
    val textValue: StateFlow<TextFieldValue> = _textValue.asStateFlow()

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
    }

    // Toggle K without resetting active selection
    fun toggleK() { 
        _kActive.value = !_kActive.value 
    }

    // Toggle P without resetting active selection
    fun toggleP() { 
        _pActive.value = !_pActive.value 
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
        } else {
            // Deselect: return cursor to normal single caret
            selAnchor = null
            val caret = current.selection.end
            _textValue.value = current.copy(
                selection = TextRange(caret, caret),
                composition = null
            )
            _transientHighlightRange.value = null
        }
    }
    
    fun toggleKbLock() { _kbLockActive.value = !_kbLockActive.value }
    
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
            closeLanguagePicker()
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveCurrentFile(onRequireSaveAs: () -> Unit) {
        val uri = _currentFileUri.value
        if (uri != null) {
            saveToUri(uri)
        } else {
            onRequireSaveAs()
        }
    }

    fun newFile() {
        _textValue.value = TextFieldValue("")
        _currentFileUri.value = null
        _fileName.value = "newfile.txt"
        resetCursorState()
    }

    private fun getFileName(contentResolver: android.content.ContentResolver, uri: android.net.Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }

    fun onMicClicked() {
        val currentSettings = settings.value
        val hasSelection = _textValue.value.selection.length > 0
        
        if (speechWrapper.isListening.value) {
            speechWrapper.stopListening()
            return
        }

        if (hasSelection) {
            speechWrapper.clearResults()
            _showReplacePopup.value = true
        } else {
            speechWrapper.startListening(currentSettings.voiceLanguage)
        }
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
