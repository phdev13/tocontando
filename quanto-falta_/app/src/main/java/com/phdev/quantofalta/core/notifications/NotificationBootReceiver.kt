package com.phdev.quantofalta.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.phdev.quantofalta.core.notifications.worker.NotificationReschedulerWorker

class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Action recebida: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            
            Log.d("BootReceiver", "Delega reagendamento ao WorkManager para não travar a main thread")
            
            NotificationReschedulerWorker.enqueue(context)
        }
    }
}
