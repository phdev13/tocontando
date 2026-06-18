package com.phdev.quantofalta.domain.model

import androidx.compose.ui.graphics.Color
import com.phdev.quantofalta.core.designsystem.theme.Colors

data class Event(
    val id: String,
    val title: String,
    val iconName: String,
    val colorArgb: Int,
    val targetDate: java.time.LocalDate,
    val targetTime: java.time.LocalTime?,
    val zoneId: String,
    val referenceDate: java.time.LocalDate?,
    val format: CountdownFormat,
    val direction: CountdownDirection,
    val createdAtMillis: Long,
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val coverImageUri: String? = null
)
