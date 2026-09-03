package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ActionButton {
    CUT, COPY, K, P, DELETE, PASTE, ENTER
}

data class SettingsEntity(
    val id: Int = 1,
    val arrowSize: Float = 1.0f,
    val buttonOrder: String = "CUT,COPY,K,P,DELETE,PASTE,ENTER",
    val voiceLanguage: String = "en-US"
)

class SettingsRepository(private val context: Context) {
    private val ARROW_SIZE = floatPreferencesKey("arrowSize")
    private val BUTTON_ORDER = stringPreferencesKey("buttonOrder")
    private val VOICE_LANGUAGE = stringPreferencesKey("voiceLanguage")

    val settings: Flow<SettingsEntity> = context.dataStore.data.map { prefs ->
        SettingsEntity(
            arrowSize = prefs[ARROW_SIZE] ?: 1.0f,
            buttonOrder = prefs[BUTTON_ORDER] ?: "CUT,COPY,K,P,DELETE,PASTE,ENTER",
            voiceLanguage = prefs[VOICE_LANGUAGE] ?: "en-US"
        )
    }

    suspend fun updateArrowSize(size: Float) {
        context.dataStore.edit { prefs -> prefs[ARROW_SIZE] = size }
    }

    suspend fun updateButtonOrder(order: List<ActionButton>) {
        context.dataStore.edit { prefs -> 
            prefs[BUTTON_ORDER] = order.joinToString(",") { it.name } 
        }
    }

    suspend fun updateVoiceLanguage(language: String) {
        context.dataStore.edit { prefs -> prefs[VOICE_LANGUAGE] = language }
    }
}
