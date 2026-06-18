package com.phdev.quantofalta.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_TODAY = "channel_today_events"
    const val CHANNEL_UPCOMING = "channel_upcoming_reminders"
    const val CHANNEL_UPDATES = "channel_app_updates"
    const val CHANNEL_IMPORTANT = "channel_important_info"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val todayChannel = NotificationChannel(
                CHANNEL_TODAY,
                "Eventos de hoje",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas para eventos que acontecem no mesmo dia."
                enableVibration(true)
            }

            val upcomingChannel = NotificationChannel(
                CHANNEL_UPCOMING,
                "Lembretes antecipados",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos configurados antes da data do evento."
            }

            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "Atualizações do aplicativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Informações relacionadas a versões e melhorias."
            }

            val importantChannel = NotificationChannel(
                CHANNEL_IMPORTANT,
                "Informações importantes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mensagens operacionais essenciais."
            }

            notificationManager.createNotificationChannels(
                listOf(todayChannel, upcomingChannel, updatesChannel, importantChannel)
            )
        }
    }
}
