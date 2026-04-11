package com.example.liber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sources")
data class ScanSourceEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val lastScannedAt: Long? = null,
    val bookCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)
