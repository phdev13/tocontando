package com.phdev.quantofalta.core.notifications

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.notifications.model.EventReminder
import com.phdev.quantofalta.core.notifications.model.TriggerType

object NotificationDispatcher {
    private const val TAG = "NotificationDispatcher"

    suspend fun dispatch(context: Context, reminderId: String, eventId: String, triggerAt: Long) {
        val db = AppDatabase.getDatabase(context)
        if (triggerAt <= 0L || db.scheduledNotificationDao().claimForDispatching(reminderId, triggerAt) == 0) {
            Log.w(TAG, "Disparo duplicado ou obsoleto ignorado: $reminderId")
            return
        }
        val event = db.eventDao().getEventByIdSync(eventId)
        val reminderEntity = db.eventReminderDao().getRemindersForEventSync(eventId).find { it.id == reminderId }

        if (event == null || event.isArchived || event.isCompleted) {
            Log.w(TAG, "Evento inexistente ou concluído/arquivado. Ignorando.")
            db.scheduledNotificationDao().updateStatus(
                reminderId,
                com.phdev.quantofalta.core.database.NotificationStatus.SKIPPED.name,
                System.currentTimeMillis()
            )
            return
        }

        if (reminderEntity == null || !reminderEntity.enabled) {
            Log.w(TAG, "Lembrete inexistente ou desativado. Ignorando.")
            db.scheduledNotificationDao().updateStatus(
                reminderId,
                com.phdev.quantofalta.core.database.NotificationStatus.SKIPPED.name,
                System.currentTimeMillis()
            )
            return
        }

        try {
            NotificationFactory.publish(context, event, reminderEntity)
            db.scheduledNotificationDao().updateStatus(
                reminderId, com.phdev.quantofalta.core.database.NotificationStatus.TRIGGERED.name,
                System.currentTimeMillis()
            )
        } catch (error: Exception) {
            db.scheduledNotificationDao().updateStatus(
                reminderId, com.phdev.quantofalta.core.database.NotificationStatus.FAILED.name,
                System.currentTimeMillis()
            )
            throw error
        }
    }

    suspend fun dispatchCompletion(context: Context, eventId: String, triggerAt: Long) {
        val db = AppDatabase.getDatabase(context)
        val scheduleId = "COMPLETION_$eventId"
        if (triggerAt <= 0L || db.scheduledNotificationDao().claimForDispatching(scheduleId, triggerAt) == 0) {
            Log.w(TAG, "Conclusão duplicada ou obsoleta ignorada: $eventId")
            return
        }
        val event = db.eventDao().getEventByIdSync(eventId)

        if (event == null || event.isArchived || event.isCompleted) {
            Log.w(TAG, "Evento inexistente ou já concluído. Ignorando conclusão.")
            db.scheduledNotificationDao().updateStatus(
                "COMPLETION_$eventId",
                com.phdev.quantofalta.core.database.NotificationStatus.SKIPPED.name,
                System.currentTimeMillis()
            )
            return
        }

        val app = context.applicationContext as? com.phdev.quantofalta.ToContandoApplication
        if (app != null) {
            app.container.eventRepository.markEventAsCompleted(eventId, true)
        } else {
            db.eventDao().updateEventCompletedStatus(eventId, true)
        }

        NotificationFactory.publishCompletion(context, event)
        db.scheduledNotificationDao().updateStatus(
            scheduleId, com.phdev.quantofalta.core.database.NotificationStatus.TRIGGERED.name,
            System.currentTimeMillis()
        )
    }
}
