package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modules")
data class ModulePackage(
    @PrimaryKey val name: String,
    val version: String,
    val description: String,
    val isInstalled: Boolean = false,
    val downloadProgress: Float = 0.0f,
    val size: String,
    val category: String
)
