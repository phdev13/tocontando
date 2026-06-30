package com.phdev.quantofalta.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.phdev.quantofalta.core.database.EventEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object SmartNotificationManager {
    private const val TAG = "SmartNotificationManager"
    const val ACTION_SMART_NOTIFICATION = "ACTION_SMART_NOTIFICATION"

    fun scheduleSmartNotifications(context: Context, event: EventEntity) {
        if (event.isCompleted || event.isArchived) {
            cancelSmartNotifications(context, event.id)
            return
        }

        val zone = ZoneId.of(event.zoneId)
        val now = ZonedDateTime.now(zone)

        val milestones = when (event.type) {
            "STANDARD" -> calculateStandardMilestones(event, zone, now)
            "RELATIONSHIP" -> calculateRelationshipMilestones(event, zone, now)
            "SALARY" -> calculateSalaryMilestones(event, zone, now)
            else -> emptyList()
        }

        // Cancel previous ones for this event (dynamically we just overwrite since we use deterministic IDs, but it's cleaner to just schedule the valid ones)
        // Since we cannot easily list all pending intents for a specific event without keeping a DB table, we rely on the deterministic ID. 
        // We can just schedule the new ones. Old ones in the past won't trigger anyway, but if a date changes, old future milestones might trigger.
        // It's safer to cancel known possible milestones first.
        cancelAllKnownMilestones(context, event)

        milestones.forEach { milestone ->
            scheduleAlarm(context, event, milestone.id, milestone.triggerAtMillis)
        }
    }

    fun cancelSmartNotifications(context: Context, eventId: String) {
        val dummyEvent = EventEntity(
            id = eventId, title = "", colorArgb = 0, iconName = "",
            targetDate = 0, targetTime = null, zoneId = ZoneId.systemDefault().id,
            referenceDate = null, format = "", direction = "", createdAtMillis = 0,
            isCompleted = false, isArchived = false
        )
        cancelAllKnownMilestones(context, dummyEvent)
    }

    private fun cancelAllKnownMilestones(context: Context, event: EventEntity) {
        val possibleMilestones = listOf(
            "30d_before", "7d_before", "3d_before", "1d_before", "0d_day", "1d_after",
            "7_days", "1_month", "3_months", "6_months", "1_year", "500_days", "1000_days",
            "half_cycle"
        )
        // Also years up to 100 just to be safe
        val years = (2..100).map { "${it}_years" }
        
        val allPossible = possibleMilestones + years

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SMART_NOTIFICATION
        }

        allPossible.forEach { milestoneId ->
            val pendingIntent = getPendingIntent(context, intent, event.id, milestoneId)
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun scheduleAlarm(context: Context, event: EventEntity, milestoneId: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        var canScheduleExact = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExact = alarmManager.canScheduleExactAlarms()
        }

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SMART_NOTIFICATION
            putExtra("EXTRA_EVENT_ID", event.id)
            putExtra("EXTRA_MILESTONE_ID", milestoneId)
        }

        val pendingIntent = getPendingIntent(context, intent, event.id, milestoneId)

        try {
            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
            Log.d(TAG, "Agendado smart notification ${event.id} - $milestoneId para $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "Falha de permissão ao agendar smart notification: ${e.message}")
        }
    }

    private fun getPendingIntent(context: Context, intent: Intent, eventId: String, milestoneId: String): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val requestCode = NotificationIds.fromKey("smart:$eventId:$milestoneId")
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private data class Milestone(val id: String, val triggerAtMillis: Long)

    private fun calculateStandardMilestones(event: EventEntity, zone: ZoneId, now: ZonedDateTime): List<Milestone> {
        val targetDate = LocalDate.ofEpochDay(event.targetDate)
        val milestones = mutableListOf<Milestone>()

        // Notifications at 09:00
        fun addMilestone(id: String, date: LocalDate) {
            val triggerTime = date.atTime(9, 0).atZone(zone)
            if (triggerTime.isAfter(now)) {
                milestones.add(Milestone(id, triggerTime.toInstant().toEpochMilli()))
            }
        }

        addMilestone("30d_before", targetDate.minusDays(30))
        addMilestone("7d_before", targetDate.minusDays(7))
        addMilestone("3d_before", targetDate.minusDays(3))
        addMilestone("1d_before", targetDate.minusDays(1))
        addMilestone("0d_day", targetDate)
        addMilestone("1d_after", targetDate.plusDays(1))

        return milestones
    }

    private fun calculateRelationshipMilestones(event: EventEntity, zone: ZoneId, now: ZonedDateTime): List<Milestone> {
        val startDate = LocalDate.ofEpochDay(event.relationshipStartEpochDay ?: event.targetDate)
        val milestones = mutableListOf<Milestone>()

        fun addMilestone(id: String, date: LocalDate) {
            val triggerTime = date.atTime(9, 0).atZone(zone)
            if (triggerTime.isAfter(now)) {
                milestones.add(Milestone(id, triggerTime.toInstant().toEpochMilli()))
            }
        }

        if (event.relationshipMilestonesEnabled) {
            addMilestone("7_days", startDate.plusDays(7))
            addMilestone("500_days", startDate.plusDays(500))
            addMilestone("1000_days", startDate.plusDays(1000))
        }

        if (event.relationshipMonthlyEnabled) {
            addMilestone("1_month", startDate.plusMonths(1))
            addMilestone("3_months", startDate.plusMonths(3))
            addMilestone("6_months", startDate.plusMonths(6))
        }

        if (event.relationshipAnnualEnabled) {
            // Find next anniversary
            var nextAnniversary = startDate.plusYears(1)
            var years = 1
            while (!nextAnniversary.atTime(9, 0).atZone(zone).isAfter(now) && years < 100) {
                years++
                nextAnniversary = startDate.plusYears(years.toLong())
            }
            if (years == 1) {
                addMilestone("1_year", nextAnniversary)
            } else if (years < 100) {
                addMilestone("${years}_years", nextAnniversary)
            }
        }

        return milestones
    }

    private fun calculateSalaryMilestones(event: EventEntity, zone: ZoneId, now: ZonedDateTime): List<Milestone> {
        val targetDate = LocalDate.ofEpochDay(event.salaryPaymentDateEpochDay ?: event.targetDate)
        val milestones = mutableListOf<Milestone>()

        fun addMilestone(id: String, date: LocalDate) {
            val triggerTime = date.atTime(9, 0).atZone(zone)
            if (triggerTime.isAfter(now)) {
                milestones.add(Milestone(id, triggerTime.toInstant().toEpochMilli()))
            }
        }

        addMilestone("7d_before", targetDate.minusDays(7))
        addMilestone("3d_before", targetDate.minusDays(3))
        addMilestone("1d_before", targetDate.minusDays(1))
        addMilestone("0d_day", targetDate)
        addMilestone("1d_after", targetDate.plusDays(1))

        // Half cycle logic (approximate: minus 15 days for monthly, minus 7 for biweekly)
        val halfCycleDate = when (event.salaryFrequency) {
            "monthly" -> targetDate.minusDays(15)
            "biweekly" -> targetDate.minusDays(7)
            "weekly" -> targetDate.minusDays(3)
            "custom" -> targetDate.minusDays((event.salaryCustomIntervalDays ?: 30).toLong() / 2)
            else -> targetDate.minusDays(15)
        }
        addMilestone("half_cycle", halfCycleDate)

        return milestones
    }
}
