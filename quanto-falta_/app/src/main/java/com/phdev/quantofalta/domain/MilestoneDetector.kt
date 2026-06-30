package com.phdev.quantofalta.domain

import com.phdev.quantofalta.domain.model.CountdownResult
import com.phdev.quantofalta.domain.model.Event

object MilestoneDetector {

    fun detectMilestones(
        event: Event,
        result: CountdownResult,
        previousDays: Int? = null
    ): List<String> {
        val milestones = mutableListOf<String>()

        when (result) {
            is CountdownResult.Percentage -> {
                if (result.percent >= 25f) milestones.add("PERCENTAGE_25")
                if (result.percent >= 50f) milestones.add("PERCENTAGE_50")
                if (result.percent >= 75f) milestones.add("PERCENTAGE_75")
                if (result.percent >= 100f) milestones.add("PERCENTAGE_100")
            }
            is CountdownResult.Days -> {
                if (result.direction == com.phdev.quantofalta.domain.model.CountdownDirection.REMAINING) {
                    val currentDays = result.days.toInt()
                    val threshold = listOf(100, 30, 7, 1).firstOrNull { value ->
                        if (previousDays == null) {
                            currentDays == value
                        } else {
                            previousDays > value && currentDays <= value
                        }
                    }
                    if (threshold != null) milestones.add("DAYS_REMAINING_$threshold")
                }
            }
            is CountdownResult.Months -> {
                if (result.direction == com.phdev.quantofalta.domain.model.CountdownDirection.REMAINING) {
                    if (result.months <= 6) milestones.add("MONTHS_REMAINING_6")
                    if (result.months <= 1) milestones.add("MONTHS_REMAINING_1")
                }
            }
            CountdownResult.Today -> {
                milestones.add("TODAY")
            }
            else -> {}
        }
        
        return milestones.takeLast(1)
    }
}
