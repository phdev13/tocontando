package com.phdev.quantofalta.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_timeline",
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
data class EventTimelineEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val type: String,
    val description: String,
    val timestampMillis: Long
)
