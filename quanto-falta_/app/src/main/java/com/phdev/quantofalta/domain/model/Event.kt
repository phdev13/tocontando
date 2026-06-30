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
    val updatedAtMillis: Long = createdAtMillis,
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val coverImageUri: String? = null,
    val standardModeStyle: com.phdev.quantofalta.domain.model.mode.StandardCardStyle = com.phdev.quantofalta.domain.model.mode.StandardCardStyle.CLASSIC,
    val relationshipModeStyle: com.phdev.quantofalta.domain.model.mode.RelationshipCardStyle = com.phdev.quantofalta.domain.model.mode.RelationshipCardStyle.HEART,
    val type: EventType = EventType.STANDARD,

    // Relationship Mode (null = normal event)
    val relationshipType: String? = null,
    val relationshipStartEpochDay: Long? = null,
    val relationshipMonthlyEnabled: Boolean = false,
    val relationshipAnnualEnabled: Boolean = true,
    val relationshipMilestonesEnabled: Boolean = true,
    
    // Salary Mode fields (null = normal event)
    val salaryFrequency: String? = null,
    val salaryPaymentDay: Int? = null,
    val salaryPaymentDateEpochDay: Long? = null,
    val salaryCustomIntervalDays: Int? = null,
    val salaryWeekendRule: String? = null,
    val salaryShowBusinessDays: Boolean = false,
    val salaryValue: Double? = null,
    val salaryModeStyle: com.phdev.quantofalta.domain.model.mode.SalaryCardStyle = com.phdev.quantofalta.domain.model.mode.SalaryCardStyle.NEXT_SALARY,
    val salaryGoalTarget: Double? = null,
    val salaryCustomPhrase: String? = null,
    val lastCelebrationEpochDay: Long? = null
) {
    fun isSalaryEvent(): Boolean = type == EventType.SALARY
    fun isRelationshipEvent(): Boolean = type == EventType.RELATIONSHIP
}
