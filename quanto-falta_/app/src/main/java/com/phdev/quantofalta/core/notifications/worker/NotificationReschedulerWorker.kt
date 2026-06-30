package com.phdev.quantofalta.core.notifications.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.notifications.NotificationScheduler
import androidx.glance.appwidget.updateAll
import com.phdev.quantofalta.feature.widget.EventWidget

class NotificationReschedulerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val UNIQUE_NAME = "notification_reconciler"

        fun enqueue(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<NotificationReschedulerWorker>().build()
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.d("ReschedulerWorker", "Iniciando reagendamento de notificações...")
        val db = AppDatabase.getDatabase(context)
        
        return try {
            val allEvents = db.eventDao().getAllEventsSync()
            val allReminders = db.eventReminderDao().getAllRemindersSync()
            val scheduledDao = db.scheduledNotificationDao()
            
            val validEventIds = allEvents.map { it.id }.toSet()
            
            // 1. Limpar órfãos
            val allScheduled = scheduledDao.getAll()
            var orphansCleaned = 0
            for (scheduled in allScheduled) {
                if (!validEventIds.contains(scheduled.eventId)) {
                    // Cancela via AlarmManager e remove do DB
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val action = if (scheduled.type == "COMPLETION") "ACTION_EVENT_COMPLETED" else "ACTION_DISPATCH_REMINDER"
                    val intent = android.content.Intent(context, com.phdev.quantofalta.core.notifications.NotificationActionReceiver::class.java).apply {
                        this.action = action
                    }
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        scheduled.id.hashCode(),
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    alarmManager.cancel(pendingIntent)
                    scheduledDao.deleteById(scheduled.id)
                    orphansCleaned++
                }
            }
            
            // 2. Reagendar alarmes válidos (o próprio NotificationScheduler.schedule cancela implícitamente via FLAG_UPDATE_CURRENT, 
            // mas podemos chamar cancelReminder explícito se preferir. O NotificationScheduler já lida com update do status no BD.)
            var count = 0
            for (event in allEvents) {
                if (event.isCompleted || event.isArchived) {
                    NotificationScheduler.cancelEventCompletion(context, event.id)
                    continue
                }
                
                NotificationScheduler.scheduleEventCompletion(context, event)
                com.phdev.quantofalta.core.notifications.SmartNotificationManager.scheduleSmartNotifications(context, event)

                val eventReminders = allReminders.filter { it.eventId == event.id }
                for (reminderEntity in eventReminders) {
                    if (reminderEntity.enabled) {
                        val reminder = com.phdev.quantofalta.core.notifications.model.EventReminder(
                            id = reminderEntity.id,
                            eventId = reminderEntity.eventId,
                            triggerType = com.phdev.quantofalta.core.notifications.model.TriggerType.valueOf(reminderEntity.triggerType),
                            offsetMinutes = reminderEntity.offsetMinutes,
                            customDateTimeMillis = reminderEntity.customDateTimeMillis,
                            enabled = reminderEntity.enabled,
                            allowSnooze = reminderEntity.allowSnooze,
                            soundEnabled = reminderEntity.soundEnabled,
                            vibrationEnabled = reminderEntity.vibrationEnabled,
                            createdAtMillis = reminderEntity.createdAtMillis,
                            updatedAtMillis = reminderEntity.updatedAtMillis
                        )
                        NotificationScheduler.scheduleReminder(context, event, reminder)
                        count++
                    } else {
                        NotificationScheduler.cancelReminder(context, reminderEntity.id)
                    }
                }
            }
            
            Log.d("ReschedulerWorker", "Reagendamento concluído. $count alarmes recriados. $orphansCleaned órfãos limpos.")
            androidx.glance.appwidget.GlanceAppWidgetManager(context).getGlanceIds(EventWidget::class.java).forEach { id -> EventWidget().update(context, id) }
            Result.success()
        } catch (e: Exception) {
            Log.e("ReschedulerWorker", "Falha ao reagendar notificações", e)
            Result.retry()
        }
    }
}
