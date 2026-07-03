package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class Script(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: String,
    val description: String,
    val isDiscordBot: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
