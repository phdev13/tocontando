package com.phdev.quantofalta.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.R
import com.phdev.quantofalta.core.database.EventEntity
import com.phdev.quantofalta.core.database.EventReminderEntity
import com.phdev.quantofalta.core.notifications.model.TriggerType

object NotificationFactory {

    fun publish(context: Context, event: EventEntity, reminder: EventReminderEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannels.createAllChannels(context)

        // Title and body intelligence
        val (title, text, channelId) = buildContent(event, reminder)
        
        val finalTitle = if (event.isPrivate) "Lembrete Privado" else title
        val finalText = if (event.isPrivate) "Você tem um evento privado se aproximando. Abra para ver." else text

        // Intent to open app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("event_id", event.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openPendingIntent = PendingIntent.getActivity(context, event.id.hashCode(), openIntent, pendingIntentFlags)

        // Action: Snooze
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE_REMINDER"
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_EVENT_ID", event.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(context, reminder.id.hashCode() + 1, snoozeIntent, pendingIntentFlags)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(finalTitle)
            .setContentText(finalText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        if (event.isPrivate) {
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        }

        if (reminder.allowSnooze) {
            notificationBuilder.addAction(0, "Adiar", snoozePendingIntent)
        }

        if (!reminder.soundEnabled) {
            notificationBuilder.setSound(null)
        }
        if (!reminder.vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0L))
        }

        notificationManager.notify(reminder.id.hashCode(), notificationBuilder.build())
    }

    private fun buildContent(event: EventEntity, reminder: EventReminderEntity): Triple<String, String, String> {
        return when (reminder.triggerType) {
            TriggerType.EXACT.name -> {
                Triple("Chegou a hora", "“${event.title}” começa agora.", NotificationChannels.CHANNEL_TODAY)
            }
            TriggerType.OFFSET.name -> {
                val offset = reminder.offsetMinutes ?: 0
                when {
                    offset == 1440 -> Triple("É amanhã!", "Sua contagem para “${event.title}” finaliza amanhã.", NotificationChannels.CHANNEL_UPCOMING)
                    offset >= 10080 -> Triple("Está chegando", "Falta pouco para “${event.title}”.", NotificationChannels.CHANNEL_UPCOMING)
                    offset == 60 -> Triple("Falta pouco", "“${event.title}” começa em 1 hora.", NotificationChannels.CHANNEL_TODAY)
                    else -> Triple("Lembrete de Evento", "Faltam ${offset} minutos para “${event.title}”.", NotificationChannels.CHANNEL_TODAY)
                }
            }
            else -> {
                Triple("Lembrete", "Sobre o evento “${event.title}”.", NotificationChannels.CHANNEL_UPCOMING)
            }
        }
    }

    fun publishCompletion(context: Context, event: EventEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannels.createAllChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("event_id", event.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openPendingIntent = PendingIntent.getActivity(context, ("COMPLETED_" + event.id).hashCode(), openIntent, pendingIntentFlags)

        val finalTitle = if (event.isPrivate) "Evento Privado" else "O grande dia chegou! 🎉"
        val finalText = if (event.isPrivate) "O seu evento privado chegou ao fim." else "A contagem regressiva para “${event.title}” terminou."

        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_TODAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(finalTitle)
            .setContentText(finalText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        if (event.isPrivate) {
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        }

        notificationManager.notify(("COMPLETED_" + event.id).hashCode(), notificationBuilder.build())
    }
}
