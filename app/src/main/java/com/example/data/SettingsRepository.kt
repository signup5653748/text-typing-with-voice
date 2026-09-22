package com.example.data

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voicetype_settings")

data class StarredFolder(
    val id: String,
    val name: String,
    val pathDisplay: String,
    val uriString: String,
    val isDefault: Boolean = false
)

data class SessionDraft(
    val text: String = "",
    val fileName: String = "newfile.txt",
    val uriString: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0
)

data class SettingsEntity(
    val id: Int = 1,
    // Theme & General
    val themeName: String = "Dark",
    val highlightColorHex: Long = 0xFF38BDF8,
    val backgroundColorHex: Long = 0xFF000000,
    val textColorHex: Long = 0xFFECEEF2,
    val textSizeSp: Float = 18.0f,
    val hideHeadingSymbols: Boolean = true,
    val alwaysInsertMicDirectly: Boolean = true,
    // Speech & TTS
    val voiceLanguage: String = "en-US",
    val ttsLanguage: String = "en-US",
    val ttsVoiceName: String = "",
    val ttsEnginePackage: String = "",
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    // Layout
    val arrowSize: Float = 1.0f,
    val buttonSizeMultiplier: Float = 1.0f,
    val buttonOrder: String = "CUT,COPY,DELETE,PASTE,SELECT_ALL,ENTER",
    val hiddenElements: String = "",
    val elementLayoutOrder: String = "CUT,COPY,DELETE,PASTE,SELECT_ALL,ENTER,DPAD,READ_BTN,MIC_BTN",
    // Advanced & Granular Feedback Settings
    val advancedSettingsEnabled: Boolean = false,
    val disabledSpeechFeedbackButtons: String = "", // comma-separated button action names
    val hapticFeedbackEnabled: Boolean = true,
    // Starred Folders
    val starredFoldersJson: String = "",
    // Launch & Default Visit Screen
    val startOnReadingScreen: Boolean = false,
    // Highlight Mode: "LINE" or "WORD"
    val highlightUnit: String = "LINE"
)

class SettingsRepository(private val context: Context) {
    private val THEME_NAME = stringPreferencesKey("themeName")
    private val HIGHLIGHT_COLOR = longPreferencesKey("highlightColorHex")
    private val BACKGROUND_COLOR = longPreferencesKey("backgroundColorHex")
    private val TEXT_COLOR = longPreferencesKey("textColorHex")
    private val TEXT_SIZE = floatPreferencesKey("textSizeSp")
    private val HIDE_HEADING_SYMBOLS = booleanPreferencesKey("hideHeadingSymbols")
    private val ALWAYS_INSERT_MIC_DIRECTLY = booleanPreferencesKey("alwaysInsertMicDirectly")
    private val VOICE_LANGUAGE = stringPreferencesKey("voiceLanguage")
    private val TTS_LANGUAGE = stringPreferencesKey("ttsLanguage")
    private val TTS_VOICE_NAME = stringPreferencesKey("ttsVoiceName")
    private val TTS_ENGINE_PKG = stringPreferencesKey("ttsEnginePackage")
    private val TTS_SPEED = floatPreferencesKey("ttsSpeed")
    private val TTS_PITCH = floatPreferencesKey("ttsPitch")
    private val ARROW_SIZE = floatPreferencesKey("arrowSize")
    private val BUTTON_SIZE_MULTIPLIER = floatPreferencesKey("buttonSizeMultiplier")
    private val BUTTON_ORDER = stringPreferencesKey("buttonOrder")
    private val HIDDEN_ELEMENTS = stringPreferencesKey("hiddenElements")
    private val ELEMENT_LAYOUT_ORDER = stringPreferencesKey("elementLayoutOrder")
    private val ADVANCED_SETTINGS_ENABLED = booleanPreferencesKey("advancedSettingsEnabled")
    private val DISABLED_SPEECH_FEEDBACK_BUTTONS = stringPreferencesKey("disabledSpeechFeedbackButtons")
    private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("hapticFeedbackEnabled")
    private val START_ON_READING_SCREEN = booleanPreferencesKey("startOnReadingScreen")
    private val HIGHLIGHT_UNIT = stringPreferencesKey("highlightUnit")
    private val STARRED_FOLDERS = stringPreferencesKey("starredFolders")
    private val LAST_SESSION_TEXT = stringPreferencesKey("lastSessionText")
    private val LAST_SESSION_FILE_NAME = stringPreferencesKey("lastSessionFileName")
    private val LAST_SESSION_URI = stringPreferencesKey("lastSessionUri")
    private val LAST_SESSION_SEL_START = intPreferencesKey("lastSessionSelStart")
    private val LAST_SESSION_SEL_END = intPreferencesKey("lastSessionSelEnd")

    val sessionDraftFlow: Flow<SessionDraft?> = context.dataStore.data.map { prefs ->
        val text = prefs[LAST_SESSION_TEXT]
        if (text.isNullOrBlank()) {
            null
        } else {
            SessionDraft(
                text = text,
                fileName = prefs[LAST_SESSION_FILE_NAME] ?: "newfile.txt",
                uriString = prefs[LAST_SESSION_URI] ?: "",
                selectionStart = prefs[LAST_SESSION_SEL_START] ?: 0,
                selectionEnd = prefs[LAST_SESSION_SEL_END] ?: 0
            )
        }
    }

    val lastSessionDraft: Flow<SessionDraft> = context.dataStore.data.map { prefs ->
        SessionDraft(
            text = prefs[LAST_SESSION_TEXT] ?: "",
            fileName = prefs[LAST_SESSION_FILE_NAME] ?: "newfile.txt",
            uriString = prefs[LAST_SESSION_URI] ?: "",
            selectionStart = prefs[LAST_SESSION_SEL_START] ?: 0,
            selectionEnd = prefs[LAST_SESSION_SEL_END] ?: 0
        )
    }

    suspend fun saveSessionDraft(draft: SessionDraft) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SESSION_TEXT] = draft.text
            prefs[LAST_SESSION_FILE_NAME] = draft.fileName
            prefs[LAST_SESSION_URI] = draft.uriString
            prefs[LAST_SESSION_SEL_START] = draft.selectionStart
            prefs[LAST_SESSION_SEL_END] = draft.selectionEnd
        }
    }

    suspend fun clearSessionDraft() {
        context.dataStore.edit { prefs ->
            prefs.remove(LAST_SESSION_TEXT)
            prefs.remove(LAST_SESSION_FILE_NAME)
            prefs.remove(LAST_SESSION_URI)
            prefs.remove(LAST_SESSION_SEL_START)
            prefs.remove(LAST_SESSION_SEL_END)
        }
    }

    val settingsFlow: Flow<SettingsEntity> = context.dataStore.data.map { prefs ->
        val storedHighlight = prefs[HIGHLIGHT_COLOR]
        val activeHighlight = if (storedHighlight == null || storedHighlight == 0xFFFFD600L) 0xFF38BDF8L else storedHighlight
        SettingsEntity(
            themeName = prefs[THEME_NAME] ?: "Dark",
            highlightColorHex = activeHighlight,
            backgroundColorHex = prefs[BACKGROUND_COLOR] ?: 0xFF000000,
            textColorHex = prefs[TEXT_COLOR] ?: 0xFFECEEF2,
            textSizeSp = prefs[TEXT_SIZE] ?: 18.0f,
            hideHeadingSymbols = prefs[HIDE_HEADING_SYMBOLS] ?: true,
            alwaysInsertMicDirectly = prefs[ALWAYS_INSERT_MIC_DIRECTLY] ?: true,
            voiceLanguage = prefs[VOICE_LANGUAGE] ?: "en-US",
            ttsLanguage = prefs[TTS_LANGUAGE] ?: "en-US",
            ttsVoiceName = prefs[TTS_VOICE_NAME] ?: "",
            ttsEnginePackage = prefs[TTS_ENGINE_PKG] ?: "",
            ttsSpeed = prefs[TTS_SPEED] ?: 1.0f,
            ttsPitch = prefs[TTS_PITCH] ?: 1.0f,
            arrowSize = prefs[ARROW_SIZE] ?: 1.0f,
            buttonSizeMultiplier = prefs[BUTTON_SIZE_MULTIPLIER] ?: 1.0f,
            buttonOrder = prefs[BUTTON_ORDER] ?: "CUT,COPY,DELETE,PASTE,SELECT_ALL,ENTER",
            hiddenElements = prefs[HIDDEN_ELEMENTS] ?: "",
            elementLayoutOrder = prefs[ELEMENT_LAYOUT_ORDER] ?: "CUT,COPY,DELETE,PASTE,SELECT_ALL,ENTER,DPAD,READ_BTN,MIC_BTN",
            advancedSettingsEnabled = prefs[ADVANCED_SETTINGS_ENABLED] ?: false,
            disabledSpeechFeedbackButtons = prefs[DISABLED_SPEECH_FEEDBACK_BUTTONS] ?: "",
            hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK_ENABLED] ?: true,
            starredFoldersJson = prefs[STARRED_FOLDERS] ?: "",
            startOnReadingScreen = prefs[START_ON_READING_SCREEN] ?: false,
            highlightUnit = prefs[HIGHLIGHT_UNIT] ?: "LINE"
        )
    }

    val settings: Flow<SettingsEntity> = settingsFlow

    val starredFoldersFlow: Flow<List<StarredFolder>> = context.dataStore.data.map { prefs ->
        val raw = prefs[STARRED_FOLDERS]
        if (raw.isNullOrBlank()) {
            getDefaultStarredFolders()
        } else {
            val list = parseStarredFolders(raw)
            if (list.isEmpty()) getDefaultStarredFolders() else list
        }
    }

    val starredFolders: Flow<List<StarredFolder>> = starredFoldersFlow

    private fun getDefaultStarredFolders(): List<StarredFolder> {
        val docsDir = File(context.filesDir, "Documents").apply { mkdirs() }
        val notesDir = File(context.filesDir, "Notes").apply { mkdirs() }
        val voiceNotesDir = File(context.filesDir, "VoiceNotes").apply { mkdirs() }
        val internalDownloadsDir = File(context.filesDir, "Downloads").apply { mkdirs() }

        val dummyContent = """
            ▫ VoiceType Master Feature Test Document
            Welcome to the VoiceType testing document! This file is designed to test all heading levels, speech recognition, text-to-speech, and keypad navigation features.

            ▫▫ Quick Start Guide
            Use the 3x3 arrow cluster to move character-by-character, line-by-line (K mode), or paragraph-by-paragraph (P mode).

            ▫▫▫ Keypad and Navigation Controls
            - JUMP: Opens the Jump to headings bottom sheet.
            - TOP / END: Jumps immediately to the start or end of the document.
            - READ: Reads aloud starting from the active cursor position without pronouncing ▫️ symbols.
            - MIC: Initiates voice dictation, or voice replacement if text is currently highlighted.
            - KB_LOCK: Locks out software keyboard typing and suppresses cursor blinking while navigating.

            ▫▫ Chapter 1: Multi-Level Heading Hierarchy
            Headings are created seamlessly by prefixing lines with Unicode small square symbols (▫).

            ▫▫▫ Section 1.1: Standard Headings
            Level 1 headings use one square (▫). Level 2 headings use two squares (▫▫). Level 3 headings use three squares (▫▫▫).

            ▫▫▫ Section 1.2: Deep Nested Subsections
            ▫▫▫▫ Subsection 1.2.1: Level 4 Deep Heading
            ▫▫▫▫▫ Sub-subsection 1.2.1.1: Level 5 Deepest Heading
            The Jump to dialog dynamically indents each level and color-codes depth markers.

            ▫▫ Chapter 2: Speech & Text-to-Speech
            ▫▫▫ Section 2.1: Voice Typing
            Tap the MIC button at the bottom right to dictate text directly into the cursor position.

            ▫▫▫ Section 2.2: Voice Replacement
            Select a word or phrase, then tap MIC to speak a replacement.

            ▫▫ Chapter 3: Plain-Text Storage & Portability
            Because all headings and content are standard UTF-8 text, this file opens seamlessly in any editor.

            ▫ Summary and Verification
            All features—including keyboard lock cursor suppression, Jump to dialog, and heading symbol hiding—are ready for complete verification.
        """.trimIndent()

        // 1. Write to App Documents directory
        try {
            val sampleDoc = File(docsDir, "Headings_Demo.txt")
            if (!sampleDoc.exists()) {
                sampleDoc.writeText(dummyContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Write to App Internal Downloads directory
        try {
            val internalDownloadDoc = File(internalDownloadsDir, "VoiceType_Test_Document.txt")
            internalDownloadDoc.writeText(dummyContent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Write to App External Files Downloads directory
        val extDownloadsDir = try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.apply {
                mkdirs()
                val extDoc = File(this, "VoiceType_Test_Document.txt")
                extDoc.writeText(dummyContent)
            }
        } catch (e: Exception) {
            null
        }

        // 4. Write to Device Public Downloads directory (/sdcard/Download)
        val publicDownloadsDir = try {
            val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (pubDir != null) {
                pubDir.mkdirs()
                val pubDoc = File(pubDir, "VoiceType_Test_Document.txt")
                pubDoc.writeText(dummyContent)
                pubDir
            } else null
        } catch (e: Exception) {
            null
        }

        val downloadsPath = publicDownloadsDir?.absolutePath 
            ?: extDownloadsDir?.absolutePath 
            ?: internalDownloadsDir.absolutePath

        return listOf(
            StarredFolder(
                id = "def_downloads",
                name = "Downloads",
                pathDisplay = if (publicDownloadsDir != null) "Device / Download" else "App Storage / Downloads",
                uriString = downloadsPath,
                isDefault = true
            ),
            StarredFolder(
                id = "def_docs",
                name = "My Documents",
                pathDisplay = "App Storage / Documents",
                uriString = docsDir.absolutePath,
                isDefault = true
            ),
            StarredFolder(
                id = "def_notes",
                name = "Notes",
                pathDisplay = "App Storage / Notes",
                uriString = notesDir.absolutePath,
                isDefault = true
            ),
            StarredFolder(
                id = "def_voice",
                name = "Voice Transcripts",
                pathDisplay = "App Storage / VoiceNotes",
                uriString = voiceNotesDir.absolutePath,
                isDefault = true
            )
        )
    }

    suspend fun addStarredFolder(folder: StarredFolder) {
        context.dataStore.edit { prefs ->
            val current = parseStarredFolders(prefs[STARRED_FOLDERS])
            val updated = current.filter { it.id != folder.id && it.uriString != folder.uriString } + folder
            prefs[STARRED_FOLDERS] = serializeStarredFolders(updated)
        }
    }

    suspend fun removeStarredFolder(folderId: String) {
        context.dataStore.edit { prefs ->
            val current = parseStarredFolders(prefs[STARRED_FOLDERS])
            val updated = current.filter { it.id != folderId }
            prefs[STARRED_FOLDERS] = serializeStarredFolders(updated)
        }
    }

    private fun parseStarredFolders(json: String?): List<StarredFolder> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<StarredFolder>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StarredFolder(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Folder"),
                        pathDisplay = obj.optString("pathDisplay", ""),
                        uriString = obj.optString("uriString", ""),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeStarredFolders(list: List<StarredFolder>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("pathDisplay", item.pathDisplay)
            obj.put("uriString", item.uriString)
            obj.put("isDefault", item.isDefault)
            arr.put(obj)
        }
        return arr.toString()
    }

    suspend fun updateTheme(themeName: String, bgHex: Long, textHex: Long, hlHex: Long) {
        context.dataStore.edit { prefs ->
            prefs[THEME_NAME] = themeName
            prefs[BACKGROUND_COLOR] = bgHex
            prefs[TEXT_COLOR] = textHex
            prefs[HIGHLIGHT_COLOR] = hlHex
        }
    }

    suspend fun updateHighlightColor(colorHex: Long) {
        context.dataStore.edit { prefs -> prefs[HIGHLIGHT_COLOR] = colorHex }
    }

    suspend fun updateBackgroundColor(colorHex: Long) {
        context.dataStore.edit { prefs -> prefs[BACKGROUND_COLOR] = colorHex }
    }

    suspend fun updateTextColor(colorHex: Long) {
        context.dataStore.edit { prefs -> prefs[TEXT_COLOR] = colorHex }
    }

    suspend fun updateTextSize(sizeSp: Float) {
        context.dataStore.edit { prefs -> prefs[TEXT_SIZE] = sizeSp }
    }

    suspend fun updateVoiceLanguage(language: String) {
        context.dataStore.edit { prefs -> prefs[VOICE_LANGUAGE] = language }
    }

    suspend fun updateTtsLanguage(language: String) {
        context.dataStore.edit { prefs -> prefs[TTS_LANGUAGE] = language }
    }

    suspend fun updateTtsVoiceName(voiceName: String) {
        context.dataStore.edit { prefs -> prefs[TTS_VOICE_NAME] = voiceName }
    }

    suspend fun updateTtsEnginePackage(pkg: String) {
        context.dataStore.edit { prefs -> prefs[TTS_ENGINE_PKG] = pkg }
    }

    suspend fun updateTtsSpeed(speed: Float) {
        context.dataStore.edit { prefs -> prefs[TTS_SPEED] = speed }
    }

    suspend fun updateTtsPitch(pitch: Float) {
        context.dataStore.edit { prefs -> prefs[TTS_PITCH] = pitch }
    }

    suspend fun updateArrowSize(size: Float) {
        context.dataStore.edit { prefs -> prefs[ARROW_SIZE] = size }
    }

    suspend fun updateButtonSizeMultiplier(multiplier: Float) {
        context.dataStore.edit { prefs -> prefs[BUTTON_SIZE_MULTIPLIER] = multiplier }
    }

    suspend fun updateButtonOrder(order: List<ActionButton>) {
        context.dataStore.edit { prefs ->
            prefs[BUTTON_ORDER] = order.joinToString(",") { it.name }
        }
    }

    suspend fun updateHideHeadingSymbols(hide: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HIDE_HEADING_SYMBOLS] = hide
        }
    }

    suspend fun updateAlwaysInsertMicDirectly(always: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ALWAYS_INSERT_MIC_DIRECTLY] = always
        }
    }

    suspend fun updateElementLayoutOrder(order: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[ELEMENT_LAYOUT_ORDER] = order.joinToString(",")
        }
    }

    suspend fun toggleElementVisibility(elementName: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[HIDDEN_ELEMENTS] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
            if (current.contains(elementName)) {
                current.remove(elementName)
            } else {
                current.add(elementName)
            }
            prefs[HIDDEN_ELEMENTS] = current.joinToString(",")
        }
    }

    suspend fun updateAdvancedSettingsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ADVANCED_SETTINGS_ENABLED] = enabled
        }
    }

    suspend fun updateDisabledSpeechFeedbackButtons(disabledList: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[DISABLED_SPEECH_FEEDBACK_BUTTONS] = disabledList.joinToString(",")
        }
    }

    suspend fun updateDisabledSpeechFeedbackButtons(disabledString: String) {
        context.dataStore.edit { prefs ->
            prefs[DISABLED_SPEECH_FEEDBACK_BUTTONS] = disabledString
        }
    }

    suspend fun toggleSpeechFeedbackForButton(buttonName: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[DISABLED_SPEECH_FEEDBACK_BUTTONS] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (current.contains(buttonName)) {
                current.remove(buttonName)
            } else {
                current.add(buttonName)
            }
            prefs[DISABLED_SPEECH_FEEDBACK_BUTTONS] = current.joinToString(",")
        }
    }

    suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun updateStartOnReadingScreen(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[START_ON_READING_SCREEN] = enabled
        }
    }

    suspend fun updateHighlightUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[HIGHLIGHT_UNIT] = unit
        }
    }
}
