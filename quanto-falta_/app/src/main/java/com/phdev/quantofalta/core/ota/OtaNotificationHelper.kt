package com.phdev.quantofalta.core.ota

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.R

/**
 * Manages OTA-related system notifications:
 *  - Silent progress bar while downloading in background
 *  - Actionable "Ready to Install" tap-to-install notification
 *  - Error notification with retry CTA
 *
 * All notifications are grouped under [CHANNEL_ID] which is created lazily.
 */
object OtaNotificationHelper {

    const val CHANNEL_ID = "ota_updates"
    private const val NOTIF_ID_PROGRESS = 8100
    private const val NOTIF_ID_READY    = 8101
    private const val NOTIF_ID_ERROR    = 8102

    /** Intent extra that MainActivity checks to surface the install modal */
    const val EXTRA_OTA_READY = "ota_ready_to_install"

    // ───────────────────────────── Public API ─────────────────────────────

    /**
     * Shows or updates a silent, low-priority progress notification.
     * Only the progress number is updated — no sound/vibration on update.
     */
    fun showDownloadProgress(context: Context, versionName: String, progressPct: Int) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Baixando atualização $versionName")
            .setContentText(if (progressPct < 100) "Baixando... $progressPct%" else "Finalizando…")
            .setProgress(100, progressPct, progressPct == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)   // No sound/vibration on progress updates
            .setOngoing(true)          // Can't be swiped away mid-download
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSilent(true)
            .setGroup(CHANNEL_ID)
            .build()

        try {
            nm.notify(NOTIF_ID_PROGRESS, notif)
        } catch (_: SecurityException) { /* POST_NOTIFICATIONS denied */ }
    }

    /**
     * Replaces the progress notification with a high-priority "tap to install" notification.
     * Tapping it opens MainActivity with [EXTRA_OTA_READY] = true so the install modal appears.
     */
    fun showReadyToInstall(context: Context, versionName: String) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        // Cancel the download progress notif
        nm.cancel(NOTIF_ID_PROGRESS)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OTA_READY, true)
        }
        val pi = PendingIntent.getActivity(
            context, NOTIF_ID_READY, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✅ Atualização $versionName pronta!")
            .setContentText("Toque para instalar agora.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("O Tô Contando $versionName foi baixado e validado. Toque para instalar agora — leva menos de 10 segundos."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pi)
            .addAction(
                android.R.drawable.ic_popup_sync,
                "Instalar agora",
                pi
            )
            .setGroup(CHANNEL_ID)
            .build()

        try {
            nm.notify(NOTIF_ID_READY, notif)
        } catch (_: SecurityException) {}
    }

    /** Show a low-priority notification when download fails */
    fun showDownloadError(context: Context) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        nm.cancel(NOTIF_ID_PROGRESS)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context, NOTIF_ID_ERROR, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Erro ao baixar atualização")
            .setContentText("Toque para tentar novamente quando tiver conexão.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setGroup(CHANNEL_ID)
            .build()

        try {
            nm.notify(NOTIF_ID_ERROR, notif)
        } catch (_: SecurityException) {}
    }

    /** Cancel all OTA-related notifications */
    fun cancelAll(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(NOTIF_ID_PROGRESS)
        nm.cancel(NOTIF_ID_READY)
        nm.cancel(NOTIF_ID_ERROR)
    }

    /** Cancel only the "ready to install" notification (after user taps it) */
    fun cancelReady(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_READY)
    }

    // ─────────────────────────── Channel setup ────────────────────────────

    /** Creates the notification channel (no-op below API 26 or if already exists) */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Atualizações do app",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Progresso e status de atualizações automáticas do Tô Contando."
            setShowBadge(true)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }
}
