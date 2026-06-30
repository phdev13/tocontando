package com.phdev.quantofalta.core.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val reminderId = intent.getStringExtra("EXTRA_REMINDER_ID")
        val eventId = intent.getStringExtra("EXTRA_EVENT_ID")
        val triggerAt = intent.getLongExtra("EXTRA_TRIGGER_AT", -1L)

        Log.d("NotificationActionReceiver", "Recebido action: $action para reminder: $reminderId")

        if (action == "ACTION_DISMISS_NOTIFICATION_GROUP") {
            NotificationGrouping.dismissAll(context)
            return
        }
        if (eventId == null) return
        if (action != "ACTION_EVENT_COMPLETED" &&
            action != "ACTION_COMPLETE_EVENT" &&
            action != SmartNotificationManager.ACTION_SMART_NOTIFICATION &&
            reminderId == null
        ) return

        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                when (action) {
                    "ACTION_DISPATCH_REMINDER" -> {
                        NotificationDispatcher.dispatch(context, reminderId!!, eventId, triggerAt)
                    }
                    "ACTION_EVENT_COMPLETED" -> {
                        NotificationDispatcher.dispatchCompletion(context, eventId, triggerAt)
                    }
                    "ACTION_SNOOZE_REMINDER" -> {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(
                            reminderId?.let { NotificationIds.fromKey("reminder:$it") } ?: 0
                        )

                        if (reminderId != null) {
                            NotificationScheduler.scheduleSnooze(context, reminderId, eventId, 30 * 60 * 1000L)
                            Log.d("NotificationActionReceiver", "Snoozed reminder $reminderId for 30 minutes")
                        }
                    }
                    "ACTION_COMPLETE_EVENT" -> {
                        val app = context.applicationContext as? com.phdev.quantofalta.ToContandoApplication
                        if (app != null) {
                            app.container.eventRepository.markEventAsCompleted(eventId, true)
                        } else {
                            com.phdev.quantofalta.core.database.AppDatabase.getDatabase(context)
                                .eventDao().updateEventCompletedStatus(eventId, true)
                        }
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        reminderId?.let {
                            notificationManager.cancel(NotificationIds.fromKey("reminder:$it"))
                            notificationManager.cancel(NotificationIds.fromKey("milestone:$eventId:$it"))
                        }
                        Log.d("NotificationActionReceiver", "Evento $eventId concluído pela notificação")
                    }
                    SmartNotificationManager.ACTION_SMART_NOTIFICATION -> {
                        val milestoneId = intent.getStringExtra("EXTRA_MILESTONE_ID") ?: return@launch
                        val db = com.phdev.quantofalta.core.database.AppDatabase.getDatabase(context)
                        val event = db.eventDao().getEventByIdSync(eventId)
                        if (event == null || event.isCompleted || event.isArchived) {
                            Log.w("NotificationActionReceiver", "Evento inexistente, concluído ou arquivado. Cancelando smart notification.")
                            SmartNotificationManager.cancelSmartNotifications(context, eventId)
                            return@launch
                        }

                        val message = when (event.type) {
                            "STANDARD" -> SmartNotificationMessages.getMessageForStandard(event, milestoneId)
                            "RELATIONSHIP" -> SmartNotificationMessages.getMessageForRelationship(event, milestoneId)
                            "SALARY" -> SmartNotificationMessages.getMessageForSalary(event, milestoneId)
                            else -> SmartNotificationMessages.NotificationContent(event.title, "Lembrete: ${event.title}")
                        }

                        NotificationFactory.publishSmartNotification(context, event, message.title, message.text, milestoneId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
