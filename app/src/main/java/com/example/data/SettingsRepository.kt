package com.example.data

import android.content.Context
import android.os.Environment
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ActionButton {
    CUT, COPY, K, P, DELETE, PASTE, ENTER
}

enum class ControlElement(val label: String, val category: String) {
    CUT("Cut (CUT)", "Actions"),
    COPY("Copy (COPY)", "Actions"),
    DELETE("Delete (DEL)", "Actions"),
    PASTE("Paste (PASTE)", "Actions"),
    MORE("More Menu (...)", "Actions"),
    ENTER("Enter (↵)", "Actions"),
    K_TOGGLE("Letter K Toggle", "Modes"),
    P_TOGGLE("Letter P Toggle", "Modes"),
    KB_LOCK("Keyboard Lock (KB)", "Modes"),
    DPAD("Directional D-Pad (Arrows + SEL)", "Navigation"),
    READ_BTN("Floating Read TTS Button", "Floating"),
    MIC_BTN("Floating Voice Mic Button", "Floating")
}

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
    val themeName: String = "Dark", // "Dark", "Light", "OLED Black", "Midnight Blue", "Forest Green", "Sepia"
    val highlightColorHex: Long = 0xFFFFD600, // Bright Yellow default
    val backgroundColorHex: Long = 0xFF000000, // Black default
    val textColorHex: Long = 0xFFECEEF2, // Off-white default
    val textSizeSp: Float = 18.0f,
    // Speech & TTS
    val voiceLanguage: String = "en-US",
    val ttsLanguage: String = "en-US",
    val ttsEnginePackage: String = "",
    // Layout
    val arrowSize: Float = 1.0f,
    val buttonSizeMultiplier: Float = 1.0f, // 0.8f to 1.5f
    val buttonOrder: String = "CUT,COPY,DELETE,PASTE,MORE,ENTER",
    val hiddenElements: String = "", // comma-separated ControlElement names that user hid
    val elementLayoutOrder: String = "CUT,COPY,DELETE,PASTE,MORE,ENTER,DPAD,READ_BTN,MIC_BTN,K_TOGGLE,P_TOGGLE,KB_LOCK",
    // Starred Folders
    val starredFoldersJson: String = ""
)

class SettingsRepository(private val context: Context) {
    private val THEME_NAME = stringPreferencesKey("themeName")
    private val HIGHLIGHT_COLOR = longPreferencesKey("highlightColorHex")
    private val BACKGROUND_COLOR = longPreferencesKey("backgroundColorHex")
    private val TEXT_COLOR = longPreferencesKey("textColorHex")
    private val TEXT_SIZE = floatPreferencesKey("textSizeSp")
    private val VOICE_LANGUAGE = stringPreferencesKey("voiceLanguage")
    private val TTS_LANGUAGE = stringPreferencesKey("ttsLanguage")
    private val TTS_ENGINE_PKG = stringPreferencesKey("ttsEnginePackage")
    private val ARROW_SIZE = floatPreferencesKey("arrowSize")
    private val BUTTON_SIZE_MULTIPLIER = floatPreferencesKey("buttonSizeMultiplier")
    private val BUTTON_ORDER = stringPreferencesKey("buttonOrder")
    private val HIDDEN_ELEMENTS = stringPreferencesKey("hiddenElements")
    private val ELEMENT_LAYOUT_ORDER = stringPreferencesKey("elementLayoutOrder")
    private val STARRED_FOLDERS = stringPreferencesKey("starredFolders")
    private val LAST_SESSION_TEXT = stringPreferencesKey("lastSessionText")
    private val LAST_SESSION_FILE_NAME = stringPreferencesKey("lastSessionFileName")
    private val LAST_SESSION_URI = stringPreferencesKey("lastSessionUri")
    private val LAST_SESSION_SEL_START = intPreferencesKey("lastSessionSelStart")
    private val LAST_SESSION_SEL_END = intPreferencesKey("lastSessionSelEnd")

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

    val settings: Flow<SettingsEntity> = context.dataStore.data.map { prefs ->
        SettingsEntity(
            themeName = prefs[THEME_NAME] ?: "Dark",
            highlightColorHex = prefs[HIGHLIGHT_COLOR] ?: 0xFFFFD600,
            backgroundColorHex = prefs[BACKGROUND_COLOR] ?: 0xFF000000,
            textColorHex = prefs[TEXT_COLOR] ?: 0xFFECEEF2,
            textSizeSp = prefs[TEXT_SIZE] ?: 18.0f,
            voiceLanguage = prefs[VOICE_LANGUAGE] ?: "en-US",
            ttsLanguage = prefs[TTS_LANGUAGE] ?: "en-US",
            ttsEnginePackage = prefs[TTS_ENGINE_PKG] ?: "",
            arrowSize = prefs[ARROW_SIZE] ?: 1.0f,
            buttonSizeMultiplier = prefs[BUTTON_SIZE_MULTIPLIER] ?: 1.0f,
            buttonOrder = prefs[BUTTON_ORDER] ?: "CUT,COPY,DELETE,PASTE,MORE,ENTER",
            hiddenElements = prefs[HIDDEN_ELEMENTS] ?: "",
            elementLayoutOrder = prefs[ELEMENT_LAYOUT_ORDER] ?: "CUT,COPY,DELETE,PASTE,MORE,ENTER,DPAD,READ_BTN,MIC_BTN,K_TOGGLE,P_TOGGLE,KB_LOCK",
            starredFoldersJson = prefs[STARRED_FOLDERS] ?: ""
        )
    }

    val starredFolders: Flow<List<StarredFolder>> = context.dataStore.data.map { prefs ->
        val raw = prefs[STARRED_FOLDERS]
        if (raw.isNullOrBlank()) {
            getDefaultStarredFolders()
        } else {
            val list = parseStarredFolders(raw)
            if (list.isEmpty()) getDefaultStarredFolders() else list
        }
    }

    private fun getDefaultStarredFolders(): List<StarredFolder> {
        val docsDir = File(context.filesDir, "Documents").apply { mkdirs() }
        val notesDir = File(context.filesDir, "Notes").apply { mkdirs() }
        val voiceNotesDir = File(context.filesDir, "VoiceNotes").apply { mkdirs() }

        return listOf(
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

    suspend fun updateTtsEnginePackage(pkg: String) {
        context.dataStore.edit { prefs -> prefs[TTS_ENGINE_PKG] = pkg }
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
}
