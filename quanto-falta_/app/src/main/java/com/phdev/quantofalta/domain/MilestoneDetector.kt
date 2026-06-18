package com.phdev.quantofalta.domain

import com.phdev.quantofalta.domain.model.CountdownResult
import com.phdev.quantofalta.domain.model.Event

object MilestoneDetector {

    fun detectMilestones(event: Event, result: CountdownResult): List<String> {
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
                    if (result.days <= 100) milestones.add("DAYS_REMAINING_100")
                    if (result.days <= 30) milestones.add("DAYS_REMAINING_30")
                    if (result.days <= 7) milestones.add("DAYS_REMAINING_7")
                    if (result.days <= 1) milestones.add("DAYS_REMAINING_1")
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
        
        return milestones
    }
}
