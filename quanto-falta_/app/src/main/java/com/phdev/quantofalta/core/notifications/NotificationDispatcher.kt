package com.phdev.quantofalta.core.notifications

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.notifications.model.EventReminder
import com.phdev.quantofalta.core.notifications.model.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationDispatcher {
    private const val TAG = "NotificationDispatcher"

    fun dispatch(context: Context, reminderId: String, eventId: String) {
        val db = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val event = db.eventDao().getEventByIdSync(eventId)
            val reminderEntity = db.eventReminderDao().getRemindersForEventSync(eventId).find { it.id == reminderId }

            if (event == null || event.isArchived || event.isCompleted) {
                Log.w(TAG, "Evento inexistente ou concluído/arquivado. Ignorando.")
                return@launch
            }

            if (reminderEntity == null || !reminderEntity.enabled) {
                Log.w(TAG, "Lembrete inexistente ou desativado. Ignorando.")
                return@launch
            }

            // Publica a notificação
            NotificationFactory.publish(context, event, reminderEntity)
        }
    }
    fun dispatchCompletion(context: Context, eventId: String) {
        val db = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val event = db.eventDao().getEventByIdSync(eventId)

            if (event == null || event.isArchived || event.isCompleted) {
                Log.w(TAG, "Evento inexistente ou já concluído. Ignorando conclusão.")
                return@launch
            }

            // Mark as completed
            db.eventDao().updateEventCompletedStatus(eventId, true)

            // Show completion notification
            NotificationFactory.publishCompletion(context, event)
        }
    }
}
