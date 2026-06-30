package com.phdev.quantofalta.core.relationship

import java.time.LocalDate
import java.time.Period

/**
 * Pure, stateless calculator for Relationship Mode.
 * All calculations are deterministic given (startEpochDay, today).
 * Must be called off the main thread or inside a ViewModel.
 */
object RelationshipCalculator {

    private val MILESTONES = listOf(30, 50, 100, 180, 365, 500, 1000, 1500, 2000)

    data class RelationshipStats(
        // Raw counts
        val totalDays: Int,
        val years: Int,
        val months: Int,
        val remainingDays: Int,

        // Milestones
        val nextMilestoneDays: Int?,          // e.g. 500 (if we are at 428)
        val daysToNextMilestone: Int?,        // e.g. 72

        // Anniversaries
        val daysToNextMonthly: Int?,          // days until next monthly anniversary
        val daysToNextAnnual: Int?,           // days until next annual anniversary

        // UI helpers
        val nextSpecialEventDays: Int?,       // min(monthly, annual) — non-null
        val nextSpecialEventLabel: String?,   // "Mensário" | "1 ano" | "2 anos" etc.
    )

    fun calculate(startEpochDay: Long, today: LocalDate = LocalDate.now()): RelationshipStats {
        val startDate = LocalDate.ofEpochDay(startEpochDay)

        // Safety: if start is in the future, treat as 0 days
        if (startDate.isAfter(today)) {
            return RelationshipStats(0, 0, 0, 0, MILESTONES.firstOrNull(), MILESTONES.firstOrNull(), null, null, null, null)
        }

        val period = Period.between(startDate, today)
        val totalDays = (today.toEpochDay() - startEpochDay).toInt()

        // Next milestone
        val nextMilestone = MILESTONES.firstOrNull { it > totalDays }
        val daysToNextMilestone = nextMilestone?.let { it - totalDays }

        // Monthly anniversary: same day-of-month as start, next occurrence
        val daysToNextMonthly = calcDaysToNextMonthly(startDate, today)

        // Annual anniversary: same day+month, next year
        val daysToNextAnnual = calcDaysToNextAnnual(startDate, today)

        // Best "next special event" for the card secondary line
        val (specialDays, specialLabel) = chooseNextSpecial(
            daysToNextMonthly,
            daysToNextAnnual,
            startDate,
            today
        )

        return RelationshipStats(
            totalDays = totalDays,
            years = period.years,
            months = period.months,
            remainingDays = period.days,
            nextMilestoneDays = nextMilestone,
            daysToNextMilestone = daysToNextMilestone,
            daysToNextMonthly = daysToNextMonthly,
            daysToNextAnnual = daysToNextAnnual,
            nextSpecialEventDays = specialDays,
            nextSpecialEventLabel = specialLabel,
        )
    }

    private fun calcDaysToNextMonthly(startDate: LocalDate, today: LocalDate): Int {
        val dayOfMonth = startDate.dayOfMonth
        // Try this month
        val thisMonth = safeDate(today.year, today.monthValue, dayOfMonth)
        val nextOccurrence = if (!thisMonth.isBefore(today)) thisMonth else {
            // Move to next month
            val nextMonth = today.plusMonths(1)
            safeDate(nextMonth.year, nextMonth.monthValue, dayOfMonth)
        }
        return (nextOccurrence.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
    }

    private fun calcDaysToNextAnnual(startDate: LocalDate, today: LocalDate): Int {
        val thisYearAnniv = safeDate(today.year, startDate.monthValue, startDate.dayOfMonth)
        val nextOccurrence = if (!thisYearAnniv.isBefore(today)) thisYearAnniv else {
            safeDate(today.year + 1, startDate.monthValue, startDate.dayOfMonth)
        }
        return (nextOccurrence.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
    }

    /** Returns a valid LocalDate clamped to the last valid day of the month */
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate {
        val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
        return LocalDate.of(year, month, minOf(day, maxDay))
    }

    private fun chooseNextSpecial(
        daysToMonthly: Int?,
        daysToAnnual: Int?,
        startDate: LocalDate,
        today: LocalDate,
    ): Pair<Int?, String?> {
        if (daysToMonthly == null && daysToAnnual == null) return null to null

        // Build candidates list
        val candidates = mutableListOf<Pair<Int, String>>()

        if (daysToMonthly != null) {
            // Calculate which monthly we're about to hit (e.g. "7 meses")
            val startEpoch = startDate.toEpochDay()
            val totalDays = (today.toEpochDay() - startEpoch).toInt()
            val nextMonthOccurrence = totalDays / 30 + 1 // approximate month count
            val label = if (nextMonthOccurrence == 12) "1 ano" else "$nextMonthOccurrence meses"
            candidates.add(daysToMonthly to label)
        }

        if (daysToAnnual != null) {
            val yearsCompleted = Period.between(startDate, today).years
            val nextYear = yearsCompleted + 1
            val label = if (nextYear == 1) "1 ano" else "$nextYear anos"
            candidates.add(daysToAnnual to label)
        }

        // Pick the soonest
        val best = candidates.minByOrNull { it.first }
        return best?.first to best?.second
    }

    /**
     * Format the primary text for the card/detail screen.
     * e.g. "428 dias juntos" or "2 anos, 3 meses e 12 dias juntos"
     */
    fun formatPrimaryText(stats: RelationshipStats, relationshipType: String): String {
        val verb = when (relationshipType) {
            "married" -> "casados"
            "engaged" -> "noivos"
            else -> "juntos"
        }

        return when {
            stats.years == 0 && stats.months == 0 -> {
                val d = stats.totalDays
                "${d} ${if (d == 1) "dia" else "dias"} $verb"
            }
            stats.years == 0 -> {
                val m = stats.months
                val d = stats.remainingDays
                buildString {
                    append("${m} ${if (m == 1) "mês" else "meses"}")
                    if (d > 0) append(" e ${d} ${if (d == 1) "dia" else "dias"}")
                    append(" $verb")
                }
            }
            else -> {
                val y = stats.years
                val m = stats.months
                val d = stats.remainingDays
                buildString {
                    append("${y} ${if (y == 1) "ano" else "anos"}")
                    when {
                        m > 0 && d > 0 -> append(", ${m} ${if (m == 1) "mês" else "meses"} e ${d} ${if (d == 1) "dia" else "dias"}")
                        m > 0 -> append(" e ${m} ${if (m == 1) "mês" else "meses"}")
                        d > 0 -> append(" e ${d} ${if (d == 1) "dia" else "dias"}")
                    }
                    append(" $verb")
                }
            }
        }
    }

    /**
     * Format the secondary text for the compact card.
     * Prioritizes: milestone if sooner than anniversary, else anniversary.
     */
    fun formatSecondaryText(
        stats: RelationshipStats,
        monthlyEnabled: Boolean,
        annualEnabled: Boolean,
        milestonesEnabled: Boolean = true,
    ): String {
        val parts = mutableListOf<String>()

        // Milestone
        val milestone = stats.nextMilestoneDays
        val daysToMilestone = stats.daysToNextMilestone
        if (milestonesEnabled && milestone != null && daysToMilestone != null) {
            val dStr = if (daysToMilestone == 0) "hoje!" else "faltam $daysToMilestone"
            parts.add("Marco: $milestone dias · $dStr")
        }

        val anniversaryCandidates = buildList {
            if (monthlyEnabled) {
                stats.daysToNextMonthly?.let { add(it to "Mensário") }
            }
            if (annualEnabled) {
                stats.daysToNextAnnual?.let {
                    val nextYear = stats.years + 1
                    add(it to if (nextYear == 1) "1 ano" else "$nextYear anos")
                }
            }
        }
        anniversaryCandidates.minByOrNull { it.first }?.let { (specialDays, specialLabel) ->
            val dStr = if (specialDays == 0) "hoje!" else "em $specialDays ${if (specialDays == 1) "dia" else "dias"}"
            parts.add("$specialLabel $dStr")
        }

        return parts.firstOrNull() ?: ""
    }
}
