package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bot_configs")
data class BotConfig(
    @PrimaryKey val id: Int = 1,
    val token: String = "",
    val selectedScriptId: Int = 0,
    val status: String = "Offline", // Offline, Connecting, Online, Error
    val botName: String = "Unknown Bot",
    val messagesProcessed: Int = 0,
    val activeServers: Int = 0,
    val uptimeMillis: Long = 0
)
