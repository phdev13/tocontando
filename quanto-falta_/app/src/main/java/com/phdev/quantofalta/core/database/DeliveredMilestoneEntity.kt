package com.phdev.quantofalta.core.database

import androidx.room.Entity

@Entity(tableName = "delivered_milestones", primaryKeys = ["eventId", "milestoneKey"])
data class DeliveredMilestoneEntity(
    val eventId: String,
    val milestoneKey: String, // e.g. "PERCENTAGE_50", "DAYS_REMAINING_7"
    val deliveredAtMillis: Long
)
