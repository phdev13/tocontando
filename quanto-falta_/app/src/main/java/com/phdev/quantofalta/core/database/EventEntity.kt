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
    val standardModeStyle: String = "classic",
    val relationshipModeStyle: String = "heart",
    
    // Sync fields
    val remoteId: String? = null,
    val userId: String? = null,
    val deviceId: String = "",
    val serverUpdatedAt: Long? = null,
    val syncVersion: Int = 0,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val localRevision: Int = 1,
    val serverRevision: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val deletedByDeviceId: String? = null,
    val deleteOperationId: String? = null,

    // Relationship Mode fields (null = not a relationship event)
    val relationshipType: String? = null,          // "dating" | "married" | "engaged" | "friendship" | "other"
    val relationshipStartEpochDay: Long? = null,    // date of relationship start in epoch days
    val relationshipMonthlyEnabled: Boolean = false, // enable monthly anniversary reminders
    val relationshipAnnualEnabled: Boolean = true,   // enable annual anniversary reminders
    val relationshipMilestonesEnabled: Boolean = true, // enable milestone notifications
    
    // Salary Mode fields (null = not a salary event)
    val salaryFrequency: String? = null,           // "monthly" | "biweekly" | "weekly" | "custom"
    val salaryPaymentDay: Int? = null,             // 1-31, or epoch day for custom/weekly
    val salaryPaymentDateEpochDay: Long? = null,   // epoch day for next payment
    val salaryCustomIntervalDays: Int? = null,     // N days
    val salaryWeekendRule: String? = null,         // "keep" | "friday" | "monday"
    val salaryShowBusinessDays: Boolean = false,
    val salaryValue: Double? = null,
    val salaryModeStyle: String = "next_salary",
    val salaryGoalTarget: Double? = null,
    val salaryCustomPhrase: String? = null,
    
    // Explicit Mode/Type
    val type: String = "STANDARD",
    
    // Celebration tracking
    val lastCelebrationEpochDay: Long? = null
)
