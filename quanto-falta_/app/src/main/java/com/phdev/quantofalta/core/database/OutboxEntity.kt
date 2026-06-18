package com.phdev.quantofalta.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey
    val eventId: String,
    val op: String, // "c" for create, "u" for update, "d" for delete
    val revision: Int,
    val queuedAt: Long
)
