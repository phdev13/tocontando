package com.phdev.quantofalta.domain.model

import androidx.compose.ui.graphics.Color
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.core.designsystem.theme.Colors

data class EventUiModel(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val units: String,
    val number: String,
    val progress: Float,
    val contextMessage: String,
    val isToday: Boolean,
    val isSoon: Boolean,
    val color: Color,
    val iconName: String,
    val badgeText: String,
    val totalHoursRemaining: String,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val isPrivate: Boolean,
    val dateMillis: Long,
    val isPinned: Boolean,
    val coverImageUri: String?
)

fun Event.toUiModel(currentInstant: java.time.Instant = java.time.Instant.now(), context: android.content.Context? = null): EventUiModel {
    val calculator = com.phdev.quantofalta.domain.CountdownCalculator()
    val result = calculator.calculate(this, currentInstant)
    
    val textProvider = context?.let { com.phdev.quantofalta.presentation.CountdownTextProvider(it) }
    
    val numberStr = textProvider?.getNumberText(result) ?: "0"
    val displayUnit = textProvider?.getUnitText(result) ?: ""
    val targetZonedDateTime = java.time.ZonedDateTime.of(
        targetDate,
        targetTime ?: java.time.LocalTime.MIDNIGHT,
        java.time.ZoneId.of(zoneId)
    )
    val targetInstant = targetZonedDateTime.toInstant()

    val isDone = isCompleted || (result is com.phdev.quantofalta.domain.model.CountdownResult.Percentage && result.state == com.phdev.quantofalta.domain.model.ProgressState.COMPLETED) || targetInstant.isBefore(currentInstant)

    // Calcula o progresso visual de forma genérica (0f a 1f)
    val actualProgress = when (result) {
        is com.phdev.quantofalta.domain.model.CountdownResult.Percentage -> result.percent / 100f
        else -> {
            val createdInstant = java.time.Instant.ofEpochMilli(createdAtMillis)
            val totalMillis = targetInstant.toEpochMilli() - createdInstant.toEpochMilli()
            if (totalMillis <= 0) {
                1f
            } else {
                val elapsedMillis = currentInstant.toEpochMilli() - createdInstant.toEpochMilli()
                (elapsedMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
    
    val isToday = result is com.phdev.quantofalta.domain.model.CountdownResult.Today
    
    val hoursRemaining = java.time.Duration.between(currentInstant, targetInstant).toHours().coerceAtLeast(0)

    return EventUiModel(
        id = id.toString(),
        title = com.phdev.quantofalta.core.utils.TitleValidator.sanitizeTitle(title),
        date = if (isDone) "Concluído" else com.phdev.quantofalta.core.time.TimeUtils.formatDate(targetDate.toEpochDay() * 86400000L), // Fallback to old format logic for now
        time = if (targetTime != null) String.format("%02d:%02d", targetTime.hour, targetTime.minute) else "--:--",
        units = displayUnit,
        number = numberStr,
        progress = if (actualProgress.isNaN()) 0f else actualProgress,
        contextMessage = textProvider?.getFullText(result) ?: "Contagem regressiva",
        isToday = isToday,
        isSoon = if (isDone) false else (actualProgress > 0.9f && !isToday),
        color = androidx.compose.ui.graphics.Color(colorArgb),
        iconName = iconName,
        badgeText = if (isDone) "Evento concluído" else (textProvider?.getFullText(result) ?: ""),
        totalHoursRemaining = hoursRemaining.toString(),
        isCompleted = isCompleted,
        isArchived = isArchived,
        isPrivate = isPrivate,
        dateMillis = targetInstant.toEpochMilli(),
        isPinned = isPinned,
        coverImageUri = coverImageUri
    )
}
