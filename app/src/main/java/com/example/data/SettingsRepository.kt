package com.example.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    val elementLayoutOrder: String = "CUT,COPY,DELETE,PASTE,MORE,ENTER,DPAD,READ_BTN,MIC_BTN,K_TOGGLE,P_TOGGLE,KB_LOCK"
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
            elementLayoutOrder = prefs[ELEMENT_LAYOUT_ORDER] ?: "CUT,COPY,DELETE,PASTE,MORE,ENTER,DPAD,READ_BTN,MIC_BTN,K_TOGGLE,P_TOGGLE,KB_LOCK"
        )
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
