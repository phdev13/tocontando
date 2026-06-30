package com.phdev.quantofalta.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

import android.content.ContentResolver
import android.media.AudioAttributes
import android.net.Uri

object NotificationChannels {
    const val CHANNEL_TODAY = "channel_today_events_v2"
    const val CHANNEL_UPCOMING = "channel_upcoming_reminders_v2"
    const val CHANNEL_UPDATES = "channel_app_updates"
    const val CHANNEL_IMPORTANT = "channel_important_info"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/notificacao_v2")
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val todayChannel = NotificationChannel(
                CHANNEL_TODAY,
                "Eventos de hoje",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas para eventos que acontecem no mesmo dia."
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val upcomingChannel = NotificationChannel(
                CHANNEL_UPCOMING,
                "Lembretes antecipados",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos configurados antes da data do evento."
                setSound(soundUri, audioAttributes)
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
