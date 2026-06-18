package com.phdev.quantofalta.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance_metrics")
data class PerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,          // UUID for the run batch
    val metricType: String,     // e.g. "JANK", "STARTUP"
    val screenName: String,     // e.g. "Home", "EventDetails", "App"
    val interaction: String?,   // e.g. "Scrolling", null
    val totalFrames: Long,
    val jankFrames: Long,
    val durationMs: Long,       // For startup time
    val createdAtMillis: Long
)
