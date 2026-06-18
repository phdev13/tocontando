package com.phdev.quantofalta.core.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val reminderId = intent.getStringExtra("EXTRA_REMINDER_ID")
        val eventId = intent.getStringExtra("EXTRA_EVENT_ID")

        Log.d("NotificationActionReceiver", "Recebido action: $action para reminder: $reminderId")

        if (eventId == null) return
        if (action != "ACTION_EVENT_COMPLETED" && reminderId == null) return

        when (action) {
            "ACTION_DISPATCH_REMINDER" -> {
                NotificationDispatcher.dispatch(context, reminderId!!, eventId)
            }
            "ACTION_EVENT_COMPLETED" -> {
                NotificationDispatcher.dispatchCompletion(context, eventId)
            }
            "ACTION_SNOOZE_REMINDER" -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId?.hashCode() ?: 0)
                
                if (reminderId != null && eventId != null) {
                    NotificationScheduler.scheduleSnooze(context, reminderId, eventId, 30 * 60 * 1000L) // 30 minutos
                    Log.d("NotificationActionReceiver", "Snoozed reminder $reminderId for 30 minutes")
                }
            }
        }
    }
}
