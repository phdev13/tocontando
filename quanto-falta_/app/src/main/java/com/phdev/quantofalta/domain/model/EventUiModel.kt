package com.phdev.quantofalta.domain.model

import androidx.compose.ui.graphics.Color
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.core.utils.AppCopyProvider

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
    val coverImageUri: String?,
    val type: EventType = EventType.STANDARD,
    val eventState: EventState = EventState.ACTIVE,
    val primaryText: String = "",
    val secondaryText: String = "",
    val mode: com.phdev.quantofalta.domain.model.mode.CardMode = com.phdev.quantofalta.domain.model.mode.CardMode.Standard,
    val relationshipUiState: RelationshipUiState? = null,
    val salaryUiState: SalaryUiState? = null,
    val isAutoHighlighted: Boolean = false,
    val shouldShowCelebration: Boolean = false,
    val targetDate: Long = 0L,
    val referenceDate: Long? = null,
    val stableId: String = "",
    val visualKey: String = "",
    val metadata: Map<String, String> = emptyMap()
)

fun Event.toUiModel(currentInstant: java.time.Instant = java.time.Instant.now(), context: android.content.Context? = null): EventUiModel {
    val calculator = com.phdev.quantofalta.domain.CountdownCalculator()
    val result = calculator.calculate(this, currentInstant)
    
    val textProvider = AppCopyProvider
    
    val numberStr = textProvider.getNumberText(result)
    val displayUnit = textProvider.getUnitText(result)
    val targetZonedDateTime = java.time.ZonedDateTime.of(
        targetDate,
        targetTime ?: java.time.LocalTime.MIDNIGHT,
        java.time.ZoneId.of(zoneId)
    )
    val targetInstant = targetZonedDateTime.toInstant()

    val isDone = isCompleted || (result is com.phdev.quantofalta.domain.model.CountdownResult.Percentage && result.state == com.phdev.quantofalta.domain.model.ProgressState.COMPLETED) || targetInstant.isBefore(currentInstant)

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

    val eventState = when {
        isCompleted -> EventState.COMPLETED
        isToday -> EventState.TODAY
        else -> EventState.ACTIVE
    }

    val salaryUiStateObj = if (isSalaryEvent()) com.phdev.quantofalta.core.finance.SalaryCalculator.calculate(this, java.time.LocalDate.now(java.time.ZoneId.of(zoneId))) else null

    val dateFormatted = if (isSalaryEvent() && salaryUiStateObj != null) {
        com.phdev.quantofalta.core.time.TimeUtils.formatDate(java.time.LocalDate.ofEpochDay(salaryUiStateObj.nextPaymentEpochDay))
    } else if (isRelationshipEvent()) {
        "Desde ${com.phdev.quantofalta.core.time.TimeUtils.formatDate(targetDate.toEpochDay() * 86400000L)}"
    } else if (isDone) {
        "Concluído"
    } else {
        com.phdev.quantofalta.core.time.TimeUtils.formatDate(targetDate.toEpochDay() * 86400000L)
    }

    val actualStartEpochDay = relationshipStartEpochDay ?: targetDate.toEpochDay()
    val relationshipUiStateObj = if (isRelationshipEvent()) {
        val stats = com.phdev.quantofalta.core.relationship.RelationshipCalculator.calculate(actualStartEpochDay)
        com.phdev.quantofalta.domain.model.RelationshipUiState(
            primaryText = com.phdev.quantofalta.core.relationship.RelationshipCalculator.formatPrimaryText(stats, relationshipType ?: "dating"),
            secondaryText = com.phdev.quantofalta.core.relationship.RelationshipCalculator.formatSecondaryText(
                stats,
                relationshipMonthlyEnabled,
                relationshipAnnualEnabled,
                relationshipMilestonesEnabled,
            ),
            stats = stats,
            relationshipType = relationshipType ?: "dating",
            monthlyEnabled = relationshipMonthlyEnabled,
            annualEnabled = relationshipAnnualEnabled,
            milestonesEnabled = relationshipMilestonesEnabled,
            startEpochDay = actualStartEpochDay,
        )
    } else null

    val primaryText = when {
        eventState == EventState.COMPLETED -> "Concluído"
        salaryUiStateObj != null -> salaryUiStateObj.primaryText
        isRelationshipEvent() -> relationshipUiStateObj?.primaryText ?: ""
        eventState == EventState.TODAY -> "Hoje"
        else -> numberStr
    }

    val secondaryText = when {
        eventState == EventState.COMPLETED -> textProvider.getMemoryText(result, type, isCompleted)
        salaryUiStateObj != null -> salaryUiStateObj.secondaryText
        isRelationshipEvent() -> relationshipUiStateObj?.secondaryText ?: ""
        eventState == EventState.TODAY -> "Chegou o grande dia"
        else -> dateFormatted
    }

    val currentEpochDay = java.time.LocalDate.now(java.time.ZoneId.of(zoneId)).toEpochDay()
    val shouldShowCelebration = when {
        isSalaryEvent() -> {
            val isPayday = salaryUiStateObj?.daysRemaining == 0
            isPayday && lastCelebrationEpochDay != currentEpochDay
        }
        isRelationshipEvent() -> {
            val exactDays = relationshipUiStateObj?.stats?.totalDays?.toLong() ?: -1L
            val isMilestone = (exactDays > 0) && (exactDays % 365 == 0L || exactDays % 30 == 0L || exactDays == 100L || exactDays == 1000L)
            isMilestone && lastCelebrationEpochDay != currentEpochDay
        }
        else -> {
            val isFinished = eventState == EventState.COMPLETED || eventState == EventState.TODAY
            isFinished && lastCelebrationEpochDay != targetDate.toEpochDay()
        }
    }
    
    val modeObj = com.phdev.quantofalta.domain.model.mode.CardMode.fromEventType(type)

    return EventUiModel(
        id = id.toString(),
        title = com.phdev.quantofalta.core.utils.TitleValidator.sanitizeTitle(title),
        date = dateFormatted,
        time = if (targetTime != null) String.format("%02d:%02d", targetTime.hour, targetTime.minute) else "--:--",
        units = displayUnit,
        number = numberStr,
        progress = if (actualProgress.isNaN()) 0f else actualProgress,
        contextMessage = if (isSalaryEvent()) "" else textProvider.getFullText(result),
        isToday = isToday,
        isSoon = if (isDone) false else (actualProgress > 0.9f && !isToday),
        color = androidx.compose.ui.graphics.Color(colorArgb),
        iconName = iconName,
        badgeText = if (isRelationshipEvent()) {
            "Nosso tempo juntos"
        } else if (isSalaryEvent()) {
            "" // Salary mode uses businessDaysRemaining badge logic
        } else if (isDone) {
            "Evento concluído"
        } else {
            textProvider.getFullText(result)
        },
        totalHoursRemaining = hoursRemaining.toString(),
        isCompleted = isCompleted,
        isArchived = isArchived,
        isPrivate = isPrivate,
        dateMillis = targetInstant.toEpochMilli(),
        isPinned = isPinned,
        coverImageUri = coverImageUri,
        type = type,
        eventState = eventState,
        primaryText = primaryText,
        secondaryText = secondaryText,
        mode = modeObj,
        relationshipUiState = relationshipUiStateObj,
        salaryUiState = salaryUiStateObj,
        isAutoHighlighted = false,
        shouldShowCelebration = shouldShowCelebration,
        targetDate = targetDate.toEpochDay(),
        referenceDate = referenceDate?.toEpochDay(),
        stableId = id.toString(),
        visualKey = "${modeObj.technicalName}:${modeObj.modeId}:$id",
        metadata = buildMap<String, String> {
            relationshipType?.let { put("relationshipType", it) }
            salaryFrequency?.let { put("salaryFrequency", it) }
        }
    )
}
