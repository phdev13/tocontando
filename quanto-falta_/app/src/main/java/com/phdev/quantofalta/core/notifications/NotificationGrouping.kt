package com.phdev.quantofalta.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.R

object NotificationGrouping {
    const val GROUP_EVENTS = "com.phdev.quantofalta.EVENT_NOTIFICATIONS"
    private const val SUMMARY_KEY = "notification_group_summary"

    fun publishSummary(context: Context, channelId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val count = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.activeNotifications.count {
                it.notification.group == GROUP_EVENTS && it.id != NotificationIds.fromKey(SUMMARY_KEY)
            }.coerceAtLeast(1)
        } else {
            1
        }

        val openIntent = PendingIntent.getActivity(
            context,
            NotificationIds.fromKey("open:notification_summary"),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            NotificationIds.fromKey("dismiss:notification_group"),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "ACTION_DISMISS_NOTIFICATION_GROUP"
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summary = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (count == 1) "1 lembrete de evento" else "$count lembretes de eventos")
            .setContentText("Toque para conferir seus eventos.")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText("Tô Contando")
            )
            .setGroup(GROUP_EVENTS)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Dispensar tudo", dismissIntent)
            .build()

        manager.notify(NotificationIds.fromKey(SUMMARY_KEY), summary)
    }

    fun dismissAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.activeNotifications
                .filter { it.notification.group == GROUP_EVENTS }
                .forEach { manager.cancel(it.id) }
        } else {
            manager.cancelAll()
        }
    }
}
