package com.phdev.quantofalta.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationStatus {
    SCHEDULED,
    TRIGGERED,
    SNOOZED,
    CANCELLED,
    SKIPPED,
    FAILED
}

@Entity(tableName = "scheduled_notifications")
data class ScheduledNotificationEntity(
    @PrimaryKey val id: String, // eventId + "_" + type + "_" + triggerAt
    val eventId: String,
    val triggerAt: Long,
    val type: String, // e.g., "REMINDER", "COMPLETION"
    val status: String, // from NotificationStatus enum
    val isExact: Boolean,
    val snoozeCount: Int,
    val scheduledAt: Long,
    val lastTriggeredAt: Long?,
    val lastError: String?
)
