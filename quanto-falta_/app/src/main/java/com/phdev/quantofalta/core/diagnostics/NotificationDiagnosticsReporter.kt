package com.phdev.quantofalta.core.diagnostics

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.phdev.quantofalta.core.analytics.InstallationManager
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object NotificationDiagnosticsReporter {
    private const val TAG = "NotifDiagnostics"

    suspend fun report(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val scheduledDao = db.scheduledNotificationDao()
                val allScheduled = scheduledDao.getAll()
                
                val notificationManager = NotificationManagerCompat.from(context)
                val notificationsAllowed = notificationManager.areNotificationsEnabled()
                
                var exactAlarmsAllowed = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    exactAlarmsAllowed = alarmManager.canScheduleExactAlarms()
                }

                val installationId = InstallationManager.getOrCreateId(context)

                // Encontrar o próximo agendamento válido
                val nextTrigger = allScheduled
                    .map { it.triggerAt }
                    .filter { it > System.currentTimeMillis() }
                    .minOrNull()

                val payload = JSONObject().apply {
                    put("installationId", installationId)
                    put("notificationsAllowed", notificationsAllowed)
                    put("exactAlarmsAllowed", exactAlarmsAllowed)
                    put("activeSchedules", allScheduled.size)
                    put("nextTriggerAt", nextTrigger ?: JSONObject.NULL)
                    put("lastReconciliationAt", System.currentTimeMillis())
                }

                val response = ApiClient.post("/api/v1/telemetry/notifications", payload)
                if (response.statusCode in 200..299) {
                    Log.d(TAG, "Diagnóstico de notificações enviado com sucesso.")
                } else {
                    Log.e(TAG, "Falha ao enviar diagnóstico: HTTP ${response.statusCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao tentar enviar o diagnóstico de notificações", e)
            }
        }
    }
}
