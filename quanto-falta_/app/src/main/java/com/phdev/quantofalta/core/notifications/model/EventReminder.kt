package com.phdev.quantofalta.core.notifications.model

data class EventReminder(
    val id: String,
    val eventId: String,
    val triggerType: TriggerType,
    val offsetMinutes: Int? = null,
    val customDateTimeMillis: Long? = null,
    val enabled: Boolean = true,
    val allowSnooze: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
