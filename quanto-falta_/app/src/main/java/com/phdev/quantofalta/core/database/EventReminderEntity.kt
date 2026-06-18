package com.phdev.quantofalta.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_reminders",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId"])]
)
data class EventReminderEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val triggerType: String, // EXACT, OFFSET, CUSTOM
    val offsetMinutes: Int?, // e.g. 1440 (1 day), 60 (1 hour). Null if EXACT or CUSTOM
    val customDateTimeMillis: Long?, // Null if EXACT or OFFSET
    val enabled: Boolean,
    val allowSnooze: Boolean,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
