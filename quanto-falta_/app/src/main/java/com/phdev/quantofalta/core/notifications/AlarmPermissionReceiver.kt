package com.phdev.quantofalta.core.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.phdev.quantofalta.core.notifications.worker.NotificationReschedulerWorker

class AlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmPermissionReceiver", "Action recebida: $action")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d("AlarmPermissionReceiver", "Permissão de alarme exato concedida. Reagendando alarmes pendentes.")
            } else {
                Log.d("AlarmPermissionReceiver", "Permissão de alarme exato revogada. Aplicando fallback inexato.")
            }
            
            // Reagendar tudo usando o Worker, que vai reavaliar os fallbacks.
            val request = OneTimeWorkRequestBuilder<NotificationReschedulerWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
