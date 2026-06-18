package com.phdev.quantofalta.domain

import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.CountdownFormat
import com.phdev.quantofalta.domain.model.CountdownResult
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.ProgressState
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CountdownCalculator {

    fun calculate(event: Event, now: Instant): CountdownResult {
        val zone = ZoneId.of(event.zoneId)
        val zonedDateTimeNow = now.atZone(zone)
        val today = zonedDateTimeNow.toLocalDate()
        
        val targetDateTime = if (event.targetTime != null) {
            event.targetDate.atTime(event.targetTime).atZone(zone)
        } else {
            event.targetDate.atTime(23, 59, 59).atZone(zone)
        }

        val direction = if (event.direction == CountdownDirection.AUTO) {
            if (now.isAfter(targetDateTime.toInstant())) CountdownDirection.ELAPSED else CountdownDirection.REMAINING
        } else {
            event.direction
        }

        // Se for hoje e estamos lidando com formatações baseadas em dia inteiro e o targetTime for null ou a diferença for mínima
        val isToday = today == event.targetDate

        return when (event.format) {
            CountdownFormat.DAYS -> {
                if (isToday) return CountdownResult.Today
                val days = Math.abs(ChronoUnit.DAYS.between(today, event.targetDate))
                CountdownResult.Days(days, direction)
            }
            CountdownFormat.FULL_TIME -> {
                val duration = java.time.Duration.between(zonedDateTimeNow, targetDateTime)
                val totalSeconds = Math.abs(duration.seconds)
                val days = totalSeconds / 86400
                val hours = ((totalSeconds % 86400) / 3600).toInt()
                val minutes = ((totalSeconds % 3600) / 60).toInt()
                
                if (days == 0L && hours == 0 && minutes == 0) return CountdownResult.Today
                CountdownResult.FullTime(days, hours, minutes)
            }
            CountdownFormat.WEEKS -> {
                if (isToday) return CountdownResult.Today
                val weeks = Math.abs(ChronoUnit.WEEKS.between(today, event.targetDate))
                CountdownResult.Weeks(weeks, direction)
            }
            CountdownFormat.WEEKS_AND_DAYS -> {
                if (isToday) return CountdownResult.Today
                val daysTotal = Math.abs(ChronoUnit.DAYS.between(today, event.targetDate))
                val weeks = daysTotal / 7
                val days = (daysTotal % 7).toInt()
                CountdownResult.WeeksAndDays(weeks, days, direction)
            }
            CountdownFormat.MONTHS -> {
                if (isToday) return CountdownResult.Today
                val months = Math.abs(ChronoUnit.MONTHS.between(today, event.targetDate))
                CountdownResult.Months(months, direction)
            }
            CountdownFormat.MONTHS_AND_DAYS -> {
                if (isToday) return CountdownResult.Today
                val period = if (direction == CountdownDirection.REMAINING) {
                    Period.between(today, event.targetDate)
                } else {
                    Period.between(event.targetDate, today)
                }
                val totalMonths = period.years * 12L + period.months
                CountdownResult.MonthsAndDays(totalMonths, period.days, direction)
            }
            CountdownFormat.WORKING_DAYS -> {
                if (isToday) return CountdownResult.Today
                val days = if (direction == CountdownDirection.REMAINING) {
                    countWorkingDays(today, event.targetDate)
                } else {
                    countWorkingDays(event.targetDate, today)
                }
                CountdownResult.WorkingDays(days, direction)
            }
            CountdownFormat.PERCENTAGE -> {
                val refDate = event.referenceDate ?: return CountdownResult.Percentage(0f, ProgressState.NOT_STARTED)
                val totalDays = ChronoUnit.DAYS.between(refDate, event.targetDate)
                if (totalDays <= 0) return CountdownResult.Percentage(100f, ProgressState.COMPLETED)
                
                val elapsedDays = ChronoUnit.DAYS.between(refDate, today)
                val raw = elapsedDays.toFloat() / totalDays.toFloat() * 100f
                val percent = raw.coerceIn(0f, 100f)
                val state = when {
                    elapsedDays <= 0 -> ProgressState.NOT_STARTED
                    elapsedDays >= totalDays -> ProgressState.COMPLETED
                    else -> ProgressState.IN_PROGRESS
                }
                CountdownResult.Percentage(percent, state)
            }
            CountdownFormat.ELAPSED_DETAILED -> {
                val refDate = event.referenceDate ?: event.targetDate
                val period = Period.between(refDate, today)
                if (period.isNegative) {
                    CountdownResult.ElapsedDetailed(0, 0, 0)
                } else {
                    CountdownResult.ElapsedDetailed(period.years, period.months, period.days)
                }
            }
            CountdownFormat.AGE -> {
                val birthDate = event.referenceDate ?: event.targetDate
                val period = Period.between(birthDate, today)
                if (period.isNegative) {
                    return CountdownResult.Age(0, 0, 0, 0)
                }
                
                // Calculate next birthday
                var nextBirthday = birthDate.withYear(today.year)
                if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today)) {
                    nextBirthday = nextBirthday.plusYears(1)
                }
                val nextBirthdayInDays = ChronoUnit.DAYS.between(today, nextBirthday)
                
                CountdownResult.Age(period.years, period.months, period.days, nextBirthdayInDays)
            }
        }
    }

    private fun countWorkingDays(from: LocalDate, to: LocalDate): Long {
        if (from.isAfter(to)) return countWorkingDays(to, from)
        
        val totalDays = ChronoUnit.DAYS.between(from, to)
        if (totalDays == 0L) return 0L
        
        val fullWeeks = totalDays / 7
        val remainder = totalDays % 7
        val fromDow = from.dayOfWeek.value // 1=Mon, 7=Sun

        var extraWorkdays = 0L
        for (i in 0 until remainder) {
            val dow = ((fromDow - 1 + i.toInt()) % 7) + 1
            if (dow <= 5) extraWorkdays++
        }
        return fullWeeks * 5 + extraWorkdays
    }
}
