package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ActionButton {
    CUT, COPY, K, P, DELETE, PASTE, ENTER
}

class SettingsRepository(private val settingsDao: SettingsDao) {

    val settings: Flow<SettingsEntity> = settingsDao.getSettings().map {
        it ?: SettingsEntity()
    }

    suspend fun updateArrowSize(size: Float) {
        val current = getCurrentOrDefault()
        settingsDao.insertOrUpdate(current.copy(arrowSize = size))
    }

    suspend fun updateButtonOrder(order: List<ActionButton>) {
        val current = getCurrentOrDefault()
        val orderString = order.joinToString(",") { it.name }
        settingsDao.insertOrUpdate(current.copy(buttonOrder = orderString))
    }

    suspend fun updateVoiceLanguage(language: String) {
        val current = getCurrentOrDefault()
        settingsDao.insertOrUpdate(current.copy(voiceLanguage = language))
    }

    private suspend fun getCurrentOrDefault(): SettingsEntity {
        return settingsDao.getSettingsSingle() ?: SettingsEntity()
    }
}
