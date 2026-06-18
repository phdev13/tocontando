package com.phdev.quantofalta.core.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

object TimeUtils {

    // Cached formatters — SimpleDateFormat is expensive to instantiate (regex compilation).
    // These are reused across all toUiModel() calls triggered by the ticker.
    private val ptBR = Locale("pt", "BR")
    private val dateFormatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", ptBR)
    private val timeFormatter = SimpleDateFormat("HH:mm", ptBR)
    private val timelineFormatter = SimpleDateFormat("dd/MM/yy 'às' HH:mm", Locale.getDefault())

    fun formatDate(millis: Long): String {
        return synchronized(dateFormatter) { dateFormatter.format(Date(millis)) }
    }

    fun formatTime(millis: Long): String {
        return synchronized(timeFormatter) { timeFormatter.format(Date(millis)) }
    }

    fun formatTimelineDate(millis: Long): String {
        return synchronized(timelineFormatter) { timelineFormatter.format(Date(millis)) }
    }

    fun getAutoUnit(targetMillis: Long, currentMillis: Long): String {
        val diff = targetMillis - currentMillis
        if (diff <= 0) return "concluído"

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        if (days >= 365) return "anos"
        if (days >= 60) return "meses"
        if (days >= 14) return "semanas"
        if (days > 0) return "dias"

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        if (hours > 0) return "horas"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (minutes > 0) return "minutos"

        return "segundos"
    }

    fun calculateDifference(targetMillis: Long, currentMillis: Long = System.currentTimeMillis(), unit: String): Long {
        val diff = targetMillis - currentMillis
        if (diff <= 0) return 0

        val actualUnit = if (unit.equals("automático", ignoreCase = true)) getAutoUnit(targetMillis, currentMillis) else unit

        return when (actualUnit.lowercase()) {
            "segundos" -> TimeUnit.MILLISECONDS.toSeconds(diff)
            "minutos" -> TimeUnit.MILLISECONDS.toMinutes(diff)
            "horas" -> TimeUnit.MILLISECONDS.toHours(diff)
            "dias" -> TimeUnit.MILLISECONDS.toDays(diff)
            "semanas" -> TimeUnit.MILLISECONDS.toDays(diff) / 7
            "meses" -> TimeUnit.MILLISECONDS.toDays(diff) / 30
            "anos" -> TimeUnit.MILLISECONDS.toDays(diff) / 365
            else -> TimeUnit.MILLISECONDS.toDays(diff)
        }
    }

    fun calculateProgress(startMillis: Long, targetMillis: Long, currentMillis: Long = System.currentTimeMillis()): Float {
        if (currentMillis >= targetMillis) return 1f
        if (currentMillis <= startMillis) return 0f

        val totalDuration = targetMillis - startMillis
        if (totalDuration <= 0) return 1f

        val passedDuration = currentMillis - startMillis
        return (passedDuration.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }

    fun isSoon(targetMillis: Long, currentMillis: Long = System.currentTimeMillis()): Boolean {
        val diff = targetMillis - currentMillis
        return diff > 0 && diff <= TimeUnit.DAYS.toMillis(3)
    }

    fun getBadgeText(targetMillis: Long, currentMillis: Long = System.currentTimeMillis()): String {
        val diff = targetMillis - currentMillis
        if (diff <= 0) return "Evento concluído"

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        var remainingDiff = diff - TimeUnit.DAYS.toMillis(days)
        val hours = TimeUnit.MILLISECONDS.toHours(remainingDiff)
        remainingDiff -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingDiff)
        remainingDiff -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingDiff)

        return if (days > 0) {
            String.format(Locale.getDefault(), "%02dd %02dh %02dm %02ds restantes", days, hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02dh %02dm %02ds restantes", hours, minutes, seconds)
        }
    }

    fun getHoursRemaining(targetMillis: Long, currentMillis: Long = System.currentTimeMillis()): Long {
        val diff = targetMillis - currentMillis
        if (diff <= 0) return 0
        return TimeUnit.MILLISECONDS.toHours(diff)
    }

    /**
     * Returns the appropriate ticker interval in milliseconds for the given display unit.
     * - Seconds/minutes visible: 1 000 ms
     * - Hours visible: 60 000 ms (1 min)
     * - Days/weeks/months/years: 300 000 ms (5 min)
     * - Completed / unknown: never update (Long.MAX_VALUE used as sentinel)
     */
    fun tickerIntervalForUnit(unit: String): Long {
        return when (unit.lowercase()) {
            "segundos", "minutos" -> 1_000L
            "horas" -> 60_000L
            else -> 300_000L
        }
    }

    fun isWorldCupActive(): Boolean {
        val endDate = 1784400000000L // July 19, 2026 (Copa do Mundo 2026)
        return System.currentTimeMillis() < endDate
    }
}
