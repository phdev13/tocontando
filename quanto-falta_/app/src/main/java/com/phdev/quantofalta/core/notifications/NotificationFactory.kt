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
import com.phdev.quantofalta.data.repository.toDomainModel
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.domain.model.EventType

object NotificationFactory {

    fun publish(context: Context, event: EventEntity, reminder: EventReminderEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannels.createAllChannels(context)

        val uiModel = event.toDomainModel().toUiModel(context = context)

        // Title and body intelligence based on exact domain context
        val (title, text, channelId) = buildDynamicReminderContent(uiModel, reminder)
        
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

        val openPendingIntent = PendingIntent.getActivity(
            context, NotificationIds.fromKey("open:${event.id}"), openIntent, pendingIntentFlags
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE_REMINDER"
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_EVENT_ID", event.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationIds.fromKey("snooze:${reminder.id}"),
            snoozeIntent,
            pendingIntentFlags
        )
        val completePendingIntent = actionPendingIntent(
            context, "ACTION_COMPLETE_EVENT", event.id, reminder.id, "complete:${event.id}:${reminder.id}"
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(finalTitle)
            .setContentText(finalText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setGroup(NotificationGrouping.GROUP_EVENTS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (event.isPrivate) {
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        }

        if (reminder.allowSnooze) {
            notificationBuilder.addAction(0, "Adiar", snoozePendingIntent)
        }
        notificationBuilder.addAction(0, "Concluir", completePendingIntent)

        if (!reminder.soundEnabled) {
            notificationBuilder.setSound(null)
        }
        if (!reminder.vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0L))
        }

        notificationManager.notify(NotificationIds.fromKey("reminder:${reminder.id}"), notificationBuilder.build())
        NotificationGrouping.publishSummary(context, channelId)
    }

    private fun buildDynamicReminderContent(uiModel: com.phdev.quantofalta.domain.model.EventUiModel, reminder: EventReminderEntity): Triple<String, String, String> {
        val iconEmoji = if (uiModel.iconName != "ic_star") " ⭐" else "" // Can append an emoji indicator if needed
        return when (reminder.triggerType) {
            TriggerType.EXACT.name -> {
                when(uiModel.type) {
                    EventType.SALARY -> Triple("Chegou a hora! 💸", "O seu evento de salário “${uiModel.title}” inicia agora.", NotificationChannels.CHANNEL_TODAY)
                    EventType.RELATIONSHIP -> Triple("Feliz dia! ❤️", "O dia de comemorar “${uiModel.title}” chegou.", NotificationChannels.CHANNEL_TODAY)
                    else -> Triple("Chegou a hora! 🎉", "O evento “${uiModel.title}” começa agora.", NotificationChannels.CHANNEL_TODAY)
                }
            }
            TriggerType.OFFSET.name -> {
                val offset = reminder.offsetMinutes ?: 0
                val humanTime = formatTimeRemaining(offset)
                
                val randomTitle = listOf("Prepare-se!", "Está quase lá!", "Não esqueça!", "Lembrete:").random()
                
                when {
                    offset == 1440 -> Triple("É amanhã!", "Falta apenas 1 dia para “${uiModel.title}”.", NotificationChannels.CHANNEL_UPCOMING)
                    offset == 60 -> Triple("Falta pouco!", "“${uiModel.title}” é daqui a 1 hora.", NotificationChannels.CHANNEL_TODAY)
                    else -> Triple(randomTitle, "Faltam $humanTime para “${uiModel.title}”.", NotificationChannels.CHANNEL_UPCOMING)
                }
            }
            else -> {
                Triple("Lembrete", "Sobre o evento “${uiModel.title}”.", NotificationChannels.CHANNEL_UPCOMING)
            }
        }
    }

    private fun formatTimeRemaining(minutes: Int): String {
        if (minutes < 60) return "$minutes minutos"
        
        val days = minutes / 1440
        val remainingMinutesAfterDays = minutes % 1440
        val hours = remainingMinutesAfterDays / 60
        val remMinutes = remainingMinutesAfterDays % 60
        
        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days dia" + (if (days > 1) "s" else ""))
        if (hours > 0) parts.add("$hours hora" + (if (hours > 1) "s" else ""))
        if (remMinutes > 0 && days == 0) parts.add("$remMinutes min")
        
        if (parts.size == 1) return parts[0]
        if (parts.size == 2) return "${parts[0]} e ${parts[1]}"
        
        val last = parts.removeLast()
        return parts.joinToString(", ") + " e " + last
    }

    fun publishCompletion(context: Context, event: EventEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannels.createAllChannels(context)
        
        val uiModel = event.toDomainModel().toUiModel(context = context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("event_id", event.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openPendingIntent = PendingIntent.getActivity(
            context, NotificationIds.fromKey("open:completion:${event.id}"), openIntent, pendingIntentFlags
        )

        val defaultTitle = if (event.isPrivate) "Evento Privado" else "O grande dia chegou! 🎉"
        val defaultText = if (event.isPrivate) "O seu evento privado chegou ao fim." else "A contagem regressiva para “${event.title}” terminou."
        
        val finalTitle = if (event.isPrivate) defaultTitle else when (uiModel.type) {
            EventType.SALARY -> "Dia de Pagamento! 💸"
            EventType.RELATIONSHIP -> "Dia Especial! ❤️"
            else -> defaultTitle
        }
        
        val finalText = if (event.isPrivate) defaultText else when (uiModel.type) {
            EventType.SALARY -> "A meta para “${event.title}” foi atingida. Verifique sua conta!"
            EventType.RELATIONSHIP -> "Feliz comemoração para “${event.title}”!"
            else -> defaultText
        }

        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_TODAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(finalTitle)
            .setContentText(finalText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setGroup(NotificationGrouping.GROUP_EVENTS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .addAction(0, "Ver evento", openPendingIntent)

        if (event.isPrivate) {
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        }

        notificationManager.notify(NotificationIds.fromKey("completion:${event.id}"), notificationBuilder.build())
        NotificationGrouping.publishSummary(context, NotificationChannels.CHANNEL_TODAY)
    }

    fun publishSmartNotification(context: Context, event: EventEntity, title: String, text: String, milestoneId: String) {
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

        val openPendingIntent = PendingIntent.getActivity(
            context, NotificationIds.fromKey("open:smart:${event.id}:$milestoneId"), openIntent, pendingIntentFlags
        )

        val finalTitle = if (event.isPrivate) "Notificação Privada" else title
        val finalText = if (event.isPrivate) "Abra o aplicativo para ver os detalhes." else text

        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_UPCOMING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(finalTitle)
            .setContentText(finalText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setGroup(NotificationGrouping.GROUP_EVENTS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (event.isPrivate) {
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        }

        notificationManager.notify(NotificationIds.fromKey("smart:${event.id}:$milestoneId"), notificationBuilder.build())
        NotificationGrouping.publishSummary(context, NotificationChannels.CHANNEL_UPCOMING)
    }

    fun completeAction(context: Context, eventId: String, sourceId: String): PendingIntent =
        actionPendingIntent(context, "ACTION_COMPLETE_EVENT", eventId, sourceId, "complete:$eventId:$sourceId")

    private fun actionPendingIntent(
        context: Context,
        action: String,
        eventId: String,
        reminderId: String?,
        key: String
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra("EXTRA_EVENT_ID", eventId)
            reminderId?.let { putExtra("EXTRA_REMINDER_ID", it) }
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, NotificationIds.fromKey(key), intent, flags)
    }
}
