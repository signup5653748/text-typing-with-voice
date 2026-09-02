package com.example.ui.screens

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionButton
import com.example.data.SettingsDatabase
import com.example.data.SettingsRepository
import com.example.logic.ArrowDirection
import com.example.logic.CursorLogic
import com.example.speech.SpeechRecognitionWrapper
import com.example.speech.TTSWrapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDao = SettingsDatabase.getDatabase(application).settingsDao()
    private val settingsRepo = SettingsRepository(settingsDao)
    
    val speechWrapper = SpeechRecognitionWrapper(application)
    val ttsWrapper = TTSWrapper(application)

    val settings = settingsRepo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.data.SettingsEntity()
    )

    private val initialText = "This is the first predefined line for testing.\n" +
            "This is the second line with some more text.\n" +
            "And this is the third line to complete the initial setup."

    private val _textValue = MutableStateFlow(TextFieldValue(initialText))
    val textValue: StateFlow<TextFieldValue> = _textValue.asStateFlow()

    private val _kActive = MutableStateFlow(false)
    val kActive: StateFlow<Boolean> = _kActive.asStateFlow()

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
    }

    fun onTextChanged(newValue: TextFieldValue) {
        _textValue.value = newValue
        resetCursorState()
    }

    fun toggleK() { _kActive.value = !_kActive.value }
    fun toggleP() { _pActive.value = !_pActive.value }
    
    fun toggleSel() { 
        _selActive.value = !_selActive.value 
        val current = _textValue.value
        if (_selActive.value) {
            selAnchor = current.selection.end
        } else {
            selAnchor = null
            _textValue.value = current.copy(selection = TextRange(current.selection.end, current.selection.end))
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
        viewModelScope.launch { settingsRepo.updateVoiceLanguage(language) }
        closeLanguagePicker()
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
        if (_selActive.value) {
            _selActive.value = false
        }
        selAnchor = null
        idealX = null
        val current = _textValue.value
        if (current.selection.start != current.selection.end) {
            _textValue.value = current.copy(selection = TextRange(current.selection.end, current.selection.end))
        }
    }

    private fun handleArrow(direction: ArrowDirection, layoutResult: androidx.compose.ui.text.TextLayoutResult? = null) {
        val (newValue, newX, newAnchor) = CursorLogic.handleArrow(
            _textValue.value,
            direction,
            _kActive.value,
            !_pActive.value,
            _selActive.value,
            layoutResult,
            idealX,
            selAnchor
        )
        _textValue.value = newValue
        idealX = newX
        selAnchor = newAnchor
    }

    fun moveLeft() = handleArrow(ArrowDirection.LEFT)
    fun moveRight() = handleArrow(ArrowDirection.RIGHT)
    fun moveUp(layoutResult: androidx.compose.ui.text.TextLayoutResult?) = handleArrow(ArrowDirection.UP, layoutResult)
    fun moveDown(layoutResult: androidx.compose.ui.text.TextLayoutResult?) = handleArrow(ArrowDirection.DOWN, layoutResult)

    fun jumpStart() {
        val end = 0
        val anchor = if (_selActive.value) (selAnchor ?: _textValue.value.selection.end) else end
        selAnchor = if (_selActive.value) anchor else null
        _textValue.value = _textValue.value.copy(selection = TextRange(anchor, end))
        idealX = null
    }

    fun jumpEnd() {
        val end = _textValue.value.text.length
        val anchor = if (_selActive.value) (selAnchor ?: _textValue.value.selection.end) else end
        selAnchor = if (_selActive.value) anchor else null
        _textValue.value = _textValue.value.copy(selection = TextRange(anchor, end))
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
                if (action != ActionButton.PASTE && action != ActionButton.ENTER) {
                    resetCursorState()
                } else {
                    selAnchor = null
                    _selActive.value = false
                    idealX = null
                }
            }
        }
    }
    
    private fun currentSelectionLength() = _textValue.value.selection.length

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

    fun updateButtonOrder(order: List<ActionButton>) {
        viewModelScope.launch {
            settingsRepo.updateButtonOrder(order)
        }
    }

    fun updateArrowSize(size: Float) {
        viewModelScope.launch {
            settingsRepo.updateArrowSize(size)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsWrapper.shutdown()
    }
}
