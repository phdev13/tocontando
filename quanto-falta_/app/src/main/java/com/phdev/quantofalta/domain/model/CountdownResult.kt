package com.phdev.quantofalta.domain.model

enum class ProgressState { NOT_STARTED, IN_PROGRESS, COMPLETED }

sealed class CountdownResult {
    // Gratuito
    data class Days(val days: Long, val direction: CountdownDirection) : CountdownResult()

    // Premium
    data class FullTime(val days: Long, val hours: Int, val minutes: Int) : CountdownResult()
    data class FullTimeWithSeconds(val days: Long, val hours: Int, val minutes: Int, val seconds: Int) : CountdownResult()
    data class Weeks(val weeks: Long, val direction: CountdownDirection) : CountdownResult()
    data class WeeksAndDays(val weeks: Long, val days: Int, val direction: CountdownDirection) : CountdownResult()
    data class Months(val months: Long, val direction: CountdownDirection) : CountdownResult()
    data class MonthsAndDays(val months: Long, val days: Int, val direction: CountdownDirection) : CountdownResult()
    data class WorkingDays(val days: Long, val direction: CountdownDirection) : CountdownResult()
    data class Percentage(
        val percent: Float,          // coerceIn(0f, 100f)
        val state: ProgressState
    ) : CountdownResult()
    data class ElapsedDetailed(val years: Int, val months: Int, val days: Int) : CountdownResult()
    data class Age(val years: Int, val months: Int, val days: Int, val nextBirthdayInDays: Long) : CountdownResult()
    
    object Today : CountdownResult()
}
