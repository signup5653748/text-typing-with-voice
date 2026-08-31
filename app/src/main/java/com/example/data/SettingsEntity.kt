package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val arrowSize: Float = 1.0f,
    val buttonOrder: String = "CUT,COPY,K,P,DELETE,PASTE,ENTER",
    val voiceLanguage: String = "en-US"
)
