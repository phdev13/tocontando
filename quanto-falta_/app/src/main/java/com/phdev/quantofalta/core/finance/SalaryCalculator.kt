package com.phdev.quantofalta.core.finance

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.phdev.quantofalta.domain.model.SalaryUiState
import com.phdev.quantofalta.domain.model.Event

object SalaryCalculator {

    fun calculate(event: Event, currentDate: LocalDate = LocalDate.now()): SalaryUiState {
        val freq = event.salaryFrequency ?: "monthly"
        val paymentDay = event.salaryPaymentDay ?: 1
        val customInterval = event.salaryCustomIntervalDays ?: 30
        val basePaymentEpoch = event.salaryPaymentDateEpochDay ?: currentDate.toEpochDay()
        val weekendRule = event.salaryWeekendRule ?: "keep"
        val showBusinessDays = event.salaryShowBusinessDays

        // 1. Calculate Next Payment Date
        val nextPaymentDate = getNextPaymentDate(freq, paymentDay, basePaymentEpoch, customInterval, weekendRule, currentDate)
        
        // 2. Adjust for weekend rule
        val adjustedDate = applyWeekendRule(nextPaymentDate, weekendRule)
        
        // 3. Days Remaining
        val daysRemaining = ChronoUnit.DAYS.between(currentDate, adjustedDate).toInt().coerceAtLeast(0)
        
        // 4. Business Days Remaining
        val businessDaysRemaining = if (showBusinessDays) calculateBusinessDays(currentDate, adjustedDate) else null
        
        // 5. Calculate Cycle Progress
        val previousPaymentDate = getPreviousPaymentDate(freq, paymentDay, basePaymentEpoch, customInterval, weekendRule, currentDate)
        val adjustedPrevious = applyWeekendRule(previousPaymentDate, weekendRule)
        
        val totalCycleDays = ChronoUnit.DAYS.between(adjustedPrevious, adjustedDate).toInt()
        val cycleProgressPercentage = if (totalCycleDays > 0) {
            val passedDays = ChronoUnit.DAYS.between(adjustedPrevious, currentDate).toInt().coerceAtLeast(0)
            ((passedDays.toFloat() / totalCycleDays) * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }

        // 6. Format UI Strings
        val primaryText = if (daysRemaining == 0) "É hoje!" else "Faltam $daysRemaining dia${if (daysRemaining > 1) "s" else ""}"
        
        val secParts = mutableListOf<String>()
        if (showBusinessDays && businessDaysRemaining != null) {
            secParts.add("$businessDaysRemaining dia${if (businessDaysRemaining > 1) "s" else ""} útei${if (businessDaysRemaining > 1) "s" else ""}")
        }
        secParts.add("ciclo $cycleProgressPercentage%")
        
        val secondaryText = secParts.joinToString(" · ")

        return SalaryUiState(
            frequency = freq,
            paymentDay = paymentDay,
            customIntervalDays = customInterval,
            nextPaymentEpochDay = adjustedDate.toEpochDay(),
            daysRemaining = daysRemaining,
            businessDaysRemaining = businessDaysRemaining,
            cycleProgressPercentage = cycleProgressPercentage,
            weekendRule = weekendRule,
            showBusinessDays = showBusinessDays,
            salaryValue = event.salaryValue,
            primaryText = primaryText,
            secondaryText = secondaryText,
            salaryCardStyle = event.salaryModeStyle,
            salaryGoalTarget = event.salaryGoalTarget,
            salaryCustomPhrase = event.salaryCustomPhrase
        )
    }

    private fun getNextPaymentDate(
        freq: String,
        paymentDay: Int,
        baseEpoch: Long,
        customInterval: Int,
        weekendRule: String,
        currentDate: LocalDate
    ): LocalDate {
        var candidate = when (freq) {
            "monthly" -> {
                // If payment day is > current month length, it takes the last valid day
                var date = LocalDate.of(currentDate.year, currentDate.monthValue, 1)
                date = date.withDayOfMonth(paymentDay.coerceAtMost(date.lengthOfMonth()))
                if (date.isBefore(currentDate) || (date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.plusMonths(1)
                    date = date.withDayOfMonth(paymentDay.coerceAtMost(date.lengthOfMonth()))
                }
                date
            }
            "biweekly" -> {
                // Find next 14 day jump from base
                var date = LocalDate.ofEpochDay(baseEpoch)
                while (date.isBefore(currentDate) || (date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.plusDays(14)
                }
                date
            }
            "weekly" -> {
                var date = LocalDate.ofEpochDay(baseEpoch)
                while (date.isBefore(currentDate) || (date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.plusDays(7)
                }
                date
            }
            "custom" -> {
                var date = LocalDate.ofEpochDay(baseEpoch)
                val interval = customInterval.coerceAtLeast(1)
                while (date.isBefore(currentDate) || (date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.plusDays(interval.toLong())
                }
                date
            }
            else -> currentDate
        }
        return candidate
    }
    
    private fun getPreviousPaymentDate(
        freq: String,
        paymentDay: Int,
        baseEpoch: Long,
        customInterval: Int,
        weekendRule: String,
        currentDate: LocalDate
    ): LocalDate {
        var candidate = when (freq) {
            "monthly" -> {
                var date = LocalDate.of(currentDate.year, currentDate.monthValue, 1)
                date = date.withDayOfMonth(paymentDay.coerceAtMost(date.lengthOfMonth()))
                if (!date.isBefore(currentDate) && !(date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.minusMonths(1)
                    date = date.withDayOfMonth(paymentDay.coerceAtMost(date.lengthOfMonth()))
                }
                date
            }
            "biweekly" -> {
                var date = LocalDate.ofEpochDay(baseEpoch)
                while (date.isBefore(currentDate) || (date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.plusDays(14)
                }
                date.minusDays(14)
            }
            "weekly" -> {
                var date = LocalDate.ofEpochDay(baseEpoch)
                while (date.isBefore(currentDate) || (date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.plusDays(7)
                }
                date.minusDays(7)
            }
            "custom" -> {
                var date = LocalDate.ofEpochDay(baseEpoch)
                val interval = customInterval.coerceAtLeast(1)
                while (date.isBefore(currentDate) || (date.isEqual(currentDate) && applyWeekendRule(date, weekendRule).isBefore(currentDate))) {
                    date = date.plusDays(interval.toLong())
                }
                date.minusDays(interval.toLong())
            }
            else -> currentDate.minusDays(30)
        }
        return candidate
    }

    private fun applyWeekendRule(date: LocalDate, rule: String): LocalDate {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> {
                if (rule == "friday") date.minusDays(1)
                else if (rule == "monday") date.plusDays(2)
                else date
            }
            DayOfWeek.SUNDAY -> {
                if (rule == "friday") date.minusDays(2)
                else if (rule == "monday") date.plusDays(1)
                else date
            }
            else -> date
        }
    }

    private fun calculateBusinessDays(start: LocalDate, end: LocalDate): Int {
        if (start.isAfter(end)) return 0
        var count = 0
        var current = start
        while (current.isBefore(end)) {
            if (current.dayOfWeek != DayOfWeek.SATURDAY && current.dayOfWeek != DayOfWeek.SUNDAY) {
                count++
            }
            current = current.plusDays(1)
        }
        return count
    }
}
