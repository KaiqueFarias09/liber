package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionaries")
data class Dictionary(
    @PrimaryKey val id: String,
    val displayName: String,
    val localAlias: String? = null,
    val sourceLanguageTag: String,
    val targetLanguageTag: String? = null,
    val dictionaryType: String,
    val packageFormat: String,
    val version: String,
    val localFilePath: String? = null,
    val remoteUrl: String? = null,
    val installSizeBytes: Long = 0,
    val isEnabled: Boolean = true,
    val priority: Int = 100,
    val installedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
