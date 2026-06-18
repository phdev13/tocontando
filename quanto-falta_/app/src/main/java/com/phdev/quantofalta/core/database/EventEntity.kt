package com.phdev.quantofalta.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val colorArgb: Int,
    val iconName: String,
    val targetDate: Long,         // Stored as epoch days
    val targetTime: Int?,         // Stored as seconds of day
    val zoneId: String,
    val referenceDate: Long?,     // Stored as epoch days
    val format: String,
    val direction: String,
    val createdAtMillis: Long,    // Keep existing naming for simplicity
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val coverImageUri: String? = null,
    
    // Sync fields
    val syncState: SyncState = SyncState.PENDING,
    val localRevision: Int = 1,
    val serverRevision: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
