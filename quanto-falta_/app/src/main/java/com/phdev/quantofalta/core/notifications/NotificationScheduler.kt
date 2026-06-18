package com.phdev.quantofalta.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.database.EventEntity
import com.phdev.quantofalta.core.database.ScheduledNotificationEntity
import com.phdev.quantofalta.core.database.NotificationStatus
import com.phdev.quantofalta.core.notifications.model.EventReminder
import com.phdev.quantofalta.core.notifications.model.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun scheduleReminder(context: Context, event: EventEntity, reminder: EventReminder) {
        if (!reminder.enabled) return
        
        val triggerTimeMillis = calculateTriggerTime(event, reminder)
        if (triggerTimeMillis <= System.currentTimeMillis()) return

        CoroutineScope(Dispatchers.IO).launch {
            scheduleInternal(
                context = context,
                id = reminder.id,
                eventId = event.id,
                triggerTimeMillis = triggerTimeMillis,
                type = "REMINDER",
                action = "ACTION_DISPATCH_REMINDER",
                extraKey = "EXTRA_REMINDER_ID",
                extraValue = reminder.id
            )
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getPendingIntentForCancel(context, reminderId.hashCode(), "ACTION_DISPATCH_REMINDER")
        alarmManager.cancel(pendingIntent)
        
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(context).scheduledNotificationDao().deleteById(reminderId)
        }
    }

    fun scheduleEventCompletion(context: Context, event: EventEntity) {
        if (event.isCompleted || event.isArchived) return
        val dateMillis = getEventMillis(event)
        if (dateMillis <= System.currentTimeMillis()) return

        val scheduleId = "COMPLETION_${event.id}"

        CoroutineScope(Dispatchers.IO).launch {
            scheduleInternal(
                context = context,
                id = scheduleId,
                eventId = event.id,
                triggerTimeMillis = dateMillis,
                type = "COMPLETION",
                action = "ACTION_EVENT_COMPLETED",
                extraKey = "EXTRA_EVENT_ID",
                extraValue = event.id
            )
        }
    }

    fun cancelEventCompletion(context: Context, eventId: String) {
        val scheduleId = "COMPLETION_$eventId"
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getPendingIntentForCancel(context, scheduleId.hashCode(), "ACTION_EVENT_COMPLETED")
        alarmManager.cancel(pendingIntent)
        
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(context).scheduledNotificationDao().deleteById(scheduleId)
        }
    }

    fun scheduleSnooze(context: Context, reminderId: String, eventId: String, delayMillis: Long = 30 * 60 * 1000L) {
        val triggerTimeMillis = System.currentTimeMillis() + delayMillis
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context).scheduledNotificationDao()
            db.incrementSnooze(reminderId)
            
            scheduleInternal(
                context = context,
                id = reminderId,
                eventId = eventId,
                triggerTimeMillis = triggerTimeMillis,
                type = "REMINDER",
                action = "ACTION_DISPATCH_REMINDER",
                extraKey = "EXTRA_REMINDER_ID",
                extraValue = reminderId
            )
        }
    }

    private suspend fun scheduleInternal(
        context: Context,
        id: String,
        eventId: String,
        triggerTimeMillis: Long,
        type: String,
        action: String,
        extraKey: String,
        extraValue: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        var canScheduleExact = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExact = alarmManager.canScheduleExactAlarms()
        }

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(extraKey, extraValue)
            if (action == "ACTION_DISPATCH_REMINDER") {
                putExtra("EXTRA_EVENT_ID", eventId)
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            flags
        )

        try {
            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                // Fallback to approximate alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            }
            
            val db = AppDatabase.getDatabase(context).scheduledNotificationDao()
            val existing = db.getById(id)
            val snoozeCount = existing?.snoozeCount ?: 0
            
            val entity = ScheduledNotificationEntity(
                id = id,
                eventId = eventId,
                triggerAt = triggerTimeMillis,
                type = type,
                status = NotificationStatus.SCHEDULED.name,
                isExact = canScheduleExact,
                snoozeCount = snoozeCount,
                scheduledAt = System.currentTimeMillis(),
                lastTriggeredAt = existing?.lastTriggeredAt,
                lastError = null
            )
            db.insertOrUpdate(entity)
            
            Log.d(TAG, "Agendado: $id para $triggerTimeMillis (Exato: $canScheduleExact)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Falha de permissão ao agendar: ${e.message}")
        }
    }

    private fun getPendingIntentForCancel(context: Context, requestCode: Int, actionStr: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = actionStr
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )
    }

    private fun calculateTriggerTime(event: EventEntity, reminder: EventReminder): Long {
        val dateMillis = getEventMillis(event)
        return when (reminder.triggerType) {
            TriggerType.EXACT -> dateMillis
            TriggerType.OFFSET -> dateMillis - ((reminder.offsetMinutes ?: 0) * 60 * 1000L)
            TriggerType.CUSTOM -> reminder.customDateTimeMillis ?: dateMillis
        }
    }
    
    private fun getEventMillis(event: EventEntity): Long {
        val zone = java.time.ZoneId.of(event.zoneId)
        val date = java.time.LocalDate.ofEpochDay(event.targetDate)
        val time = event.targetTime?.let { java.time.LocalTime.ofSecondOfDay(it.toLong()) } ?: java.time.LocalTime.of(23, 59, 59)
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}
