package com.phdev.quantofalta.core.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.analytics.InstallationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class BackupSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as ToContandoApplication
            val db = app.container.database
            val installationId = InstallationManager.getOrCreateId(applicationContext)

            // Extract all events
            val events = db.eventDao().getAllEventsSync()
            val eventsArray = JSONArray()
            
            for (event in events) {
                val obj = JSONObject()
                obj.put("id", event.id)
                obj.put("title", event.title)
                obj.put("targetDate", event.targetDate)
                if (event.targetTime != null) obj.put("targetTime", event.targetTime)
                obj.put("zoneId", event.zoneId)
                if (event.referenceDate != null) obj.put("referenceDate", event.referenceDate)
                obj.put("format", event.format)
                obj.put("direction", event.direction)
                obj.put("createdAt", event.createdAtMillis)
                obj.put("isCompleted", event.isCompleted)
                obj.put("isPrivate", event.isPrivate)
                obj.put("colorHex", event.colorArgb)
                obj.put("iconName", event.iconName)
                
                // Reminders
                val reminders = db.eventReminderDao().getRemindersForEventSync(event.id)
                val remindersArray = JSONArray()
                for (reminder in reminders) {
                    val rObj = JSONObject()
                    rObj.put("id", reminder.id)
                    rObj.put("eventId", reminder.eventId)
                    rObj.put("triggerType", reminder.triggerType)
                    rObj.put("offsetMinutes", reminder.offsetMinutes)
                    rObj.put("soundEnabled", reminder.soundEnabled)
                    rObj.put("vibrationEnabled", reminder.vibrationEnabled)
                    rObj.put("allowSnooze", reminder.allowSnooze)
                    remindersArray.put(rObj)
                }
                obj.put("reminders", remindersArray)
                eventsArray.put(obj)
            }

            val payload = JSONObject()
            payload.put("installation_id", installationId)
            payload.put("data", JSONObject().apply {
                put("events", eventsArray)
            })

            // Send to backend
            val url = URL("${BuildConfig.API_BASE_URL}/api/v1/app/sync/backup")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            if (connection.responseCode in 200..299) {
                Log.d("BackupSyncWorker", "Backup successful")
                Result.success()
            } else {
                Log.e("BackupSyncWorker", "Backup failed: ${connection.responseCode}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("BackupSyncWorker", "Backup exception", e)
            Result.retry()
        }
    }
}
